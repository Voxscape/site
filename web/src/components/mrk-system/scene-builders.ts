import type { SceneBuilder } from '../model/babylon-scene-view';
import type { Scene } from '@babylonjs/core';
import { createArcRotateCamera } from '@voxscape/vox.ts/src/demo/babylon/init-babylon';
import {
  ArcRotateCamera,
  Camera,
  Color3,
  Color4,
  CubeTexture,
  DynamicTexture,
  FreeCamera,
  PointLight,
  StandardMaterial,
  Texture,
  Vector3,
} from '@babylonjs/core';
import { HemisphericLight } from '@babylonjs/core/Lights';

export const builtinTexture = {
  uvChecker1: '/demo-models/CustomUVChecker_byValle_1K.png',
  uvChecker1transparent: '/demo-models/CustomUVChecker_byValle_1K_transparent.png',
} as const;

const builtinModel = {
  acnhTanuki: 'acnh-tanuki.glb',
} as const;

export const createShirtPreviewScene = (textureSrc?: string | File): SceneBuilder => {
  return async (sceneLoader, engine) => {
    const scene = await sceneLoader.LoadAsync('/blender/', 'shirt-split5.gltf', engine, (ev) =>
      console.debug('progress', ev),
    );
    console.debug(__filename, 'loaded', scene);
    resetColor(scene);
    // resetLight(scene);
    // activeDefaultCamera(scene);
    const cam = activeArcCamera(scene);
    if (textureSrc) {
      await replaceShirtClipTexture(scene, textureSrc);
    }

    return scene;
  };
};

export const createMaskTapeScene = (texture: string | Blob): SceneBuilder => {
  return async (sceneLoader, engine) => {
    const scene = await sceneLoader.LoadAsync('/demo-models/', 'masking-clipped-rev2.gltf', engine);

    console.debug(__filename, 'loaded', scene);

    const camera0 = scene.cameras[0];
    scene.setActiveCameraById(camera0.id);

    return scene;
  };
};

export const createAcnhPreviewScene = (texture: string | Blob): SceneBuilder => {
  return async (sceneLoader, engine) => {
    const scene = await sceneLoader.LoadAsync('/demo-models/', builtinModel.acnhTanuki, engine);

    console.debug(__filename, 'loaded', scene);
    resetColor(scene);

    const cam = activeArcCamera(scene);
    resetAcnhSceneCamera(scene, cam);

    await setTanukiClip(scene, texture);

    return scene;
  };
};

async function loadImage(res: string | Blob): Promise<HTMLImageElement> {
  return new Promise((fulfill, reject) => {
    let allocated: string | null = null;
    const img = new Image();

    const onSuccess = () => {
      if (allocated) URL.revokeObjectURL(allocated);
      fulfill(img);
    };

    const onFail = (whatever: unknown) => {
      if (allocated) URL.revokeObjectURL(allocated);
      reject(whatever);
    };

    img.onerror = onFail;
    img.onabort = onFail;
    img.onload = onSuccess;

    if (typeof res === 'string') {
      img.src = res;
    } else {
      img.src = allocated = URL.createObjectURL(res);
    }
  });
}

async function replaceShirtClipTexture(scene: Scene, textureSrc: string | File) {
  const clipMesh = scene.meshes.find((s) => s.name === 'shirt-clip');
  if (!clipMesh) {
    throw new Error(`shirt-clip mesh not found`);
  }

  const url = typeof textureSrc === 'string' ? textureSrc : URL.createObjectURL(textureSrc);
  const texture = new Texture(url, scene);
  texture.onLoadObservable.add((_, state) => {
    console.debug('texture state', state);

    if (textureSrc instanceof File) {
      URL.revokeObjectURL(url);
    }
  });

  const material = new StandardMaterial('mat-shirt-clip', scene);

  material.diffuseTexture = texture;
  // material.ambientColor = Color3.Red();

  console.debug('material', material);
  clipMesh.material = material;
}

export function resetColor(scene: Scene): void {
  scene.clearColor = Color3.Black().toColor4(0.9);
  scene.ambientColor = Color3.Yellow();
}

export function resetLight(scene: Scene): void {
  while (scene.lights.length) {
    scene.lights[0].dispose();
  }
}

export function activeArcCamera(scene: Scene): ArcRotateCamera {
  const camera0 = scene.cameras[0];
  camera0.dispose();
  const arcCam = createArcRotateCamera(scene);
  scene.setActiveCameraById(arcCam.id);
  console.debug('cameras cleared', scene.cameras);
  return arcCam;
}

export function recreateCamera(scene: Scene, original: FreeCamera): ArcRotateCamera {
  const arcCam = new ArcRotateCamera(
    'arc-rotate',
    /* alpha: rotation around "latitude axis" */ -Math.PI / 2,
    /* beta: rotation around "longitude axis" */ Math.PI / 2,
    original.position.length(),
    original.target,
    scene,
  );
  return arcCam;
}

function resetAcnhSceneCamera(scene: Scene, arcCam: ArcRotateCamera) {
  const meshCenter =
    0 &&
    scene.meshes
      .map((m) => m.position)
      .reduce((p1, p2) => p1.add(p2), Vector3.Zero())
      .scale(1 / scene.meshes.length);

  arcCam.target = new Vector3(0, 0.5, 0);
  arcCam.alpha = 3.5;
  arcCam.beta = 1.5;
  arcCam.lowerRadiusLimit = 2;
  arcCam.radius = 2;
  arcCam.upperRadiusLimit = 3;
}

/**
 * @return data uri of bitmap
 */
async function buildPaddedTexture(src: string | Blob): Promise<string> {
  const img = await loadImage(src);

  const canvas = document.createElement('canvas');
  const canvasW = (canvas.width = 1024);
  const canvasH = (canvas.height = 1024);

  const destRange = {
    dx: canvasW * 0.1,
    dw: canvasW * 0.8,
    dy: canvasH * 0.3,
    dh: canvasH * 0.6,
  };

  const ctx = canvas.getContext('2d')!;
  ctx.clearRect(0, 0, canvasW, canvasH);
  // ctx.globalAlpha = 0;
  ctx.drawImage(img, destRange.dx, destRange.dy, destRange.dw, destRange.dh);

  return canvas.toDataURL('png');
}

async function setTanukiClip(scene: Scene, textureSrc: string | Blob) {
  const tanukiClip = scene.meshes.find((m) => m.name === 'tanuki-clip');
  if (!tanukiClip) {
    throw new Error(`tanuki-clip not found`);
  }
  const material = new StandardMaterial('mat-shirt-clip', scene);

  if (1) {
    // normal texture
    const padded = await buildPaddedTexture(textureSrc);
    const texture = new Texture(padded, scene, { invertY: true });
    texture.hasAlpha = true;
    material.diffuseTexture = texture;
    material.useAlphaFromDiffuseTexture = true;
  }

  if (0) {
    // dynamic texture + draw instructions: fine
    const textureResolution = 512;
    const textureGround = new DynamicTexture('dynamic-texture', textureResolution, scene, true);
    const textureContext = textureGround.getContext();

    //Draw on canvas
    textureContext.beginPath();
    textureContext.moveTo(75 * 2, 25 * 2);
    textureContext.quadraticCurveTo(25 * 2, 25 * 2, 25 * 2, 62.5 * 2);
    textureContext.quadraticCurveTo(25 * 2, 100 * 2, 50 * 2, 100 * 2);
    textureContext.quadraticCurveTo(50 * 2, 120 * 2, 30 * 2, 125 * 2);
    textureContext.quadraticCurveTo(60 * 2, 120 * 2, 65 * 2, 100 * 2);
    textureContext.quadraticCurveTo(125 * 2, 100 * 2, 125 * 2, 62.5 * 2);
    textureContext.quadraticCurveTo(125 * 2, 25 * 2, 75 * 2, 25 * 2);
    textureContext.fillStyle = 'white';
    textureContext.fill();
    textureGround.update();

    material.diffuseTexture = textureGround;
  }

  if (0) {
    // dynamic texture + draw image: NOT WORKING
    const texture = new DynamicTexture('tanuki-clip', 64, scene);

    if (1) {
      setTimeout(async () => {
        console.log('drawing texture');

        const img = await loadImage(textureSrc);
        const ctx = texture.getContext();
        ctx.drawImage(img, 0, 0, 40, 40);
      }, 10e3);
    }

    if (0) {
      texture.drawText('QQQQQQQQQQ', 10, 10, '12px monospace', '#ff0000', null, false, true);
    }

    // material.diffuseTexture = texture;
  }
  // material.ambientColor = Color3.Red();

  console.debug('material', material);
  tanukiClip.material = material;
}

function activeDefaultCamera(scene: Scene): void {
  const camera = scene.cameras[0];
  scene.setActiveCameraById(camera.id);
}

export const loadBadgeModel: SceneBuilder = async (loader, engine) => {
  const scene = await loader.LoadAsync('/20221216-validate-3d-models/cgdata/', 'batch_fix_20221219_rev2.gltf', engine);
  console.debug('scene', scene);
  resetColor(scene);
  const topCamera = scene.cameras.find((c) => c.name === 'Camera.001')!;
  const bottomCamera = scene.cameras.find((c) => c.name === 'Camera.002');

  {
    const topHalf = scene.meshes.find((m) => m.name === 'batch_primitive0')!;
    const mat = new StandardMaterial('mat-top', scene);
    mat.ambientColor = Color3.Red();
    topHalf.material = mat;
  }
  // const recreated = recreateCamera(scene, topCamera as FreeCamera);
  scene.setActiveCameraById(topCamera.id);
  return scene;
};

export function createTopLight(scene: Scene) {
  {
    const light = new HemisphericLight('light1', new Vector3(0, -1, 1), scene);
    light.diffuse = Color3.White();
    light.specular = Color3.White();
  }
  {
    const light2 = new PointLight('light2', new Vector3(0, 10, 0), scene);
    light2.diffuse = Color3.White();
    light2.specular = Color3.White();
  }
}

export const loadCupModel: SceneBuilder = async (loader, engine) => {
  const scene = await loader.LoadAsync('/20221216-validate-3d-models/cgdata/', 'cup_fix_bare.gltf', engine);

  console.debug('scene', scene);
  resetColor(scene);

  await createSkybox(scene);
  {
    await prepareCamera(scene);
    // scene.setActiveCameraById(arc.id);
  }

  {
    createTopLight(scene);
  }

  if (1) {
    const cupMesh = scene.meshes.find((m) => m.name === 'cup')!;
    // cupMesh.scaling = Vector3.One().scale(1.5);
    const mat = new StandardMaterial('mat-top', scene);
    mat.diffuseColor = Color3.Red();
    // mat.specularColor = Color3.Red();
    // mat.ambientColor = Color3.Red();
    cupMesh.material = mat;
  }

  return scene;
};

async function createSkybox(scene: Scene) {
  const texture = CubeTexture.CreateFromPrefilteredData('https://assets.babylonjs.com/environments/studio.env', scene);
  scene.createDefaultSkybox(texture);
}

async function prepareCamera(scene: Scene) {
  scene.createDefaultCamera(true);

  const camera = scene.activeCamera! as ArcRotateCamera;

  if (1) {
    // glTF assets use a +Z forward convention while the default camera faces +Z. Rotate the camera to look at the front of the asset.
    camera.alpha += Math.PI;
  }

  if (scene.meshes.length) {
    camera.lowerRadiusLimit = null;
  }

  if (1) {
    camera.useAutoRotationBehavior = true;
  }

  camera.radius = 1;

  camera.pinchPrecision = 200 / camera.radius;
  camera.upperRadiusLimit = 5 * camera.radius;

  camera.wheelDeltaPercentage = 1e-2;
  camera.pinchDeltaPercentage = 1e-2;
}
