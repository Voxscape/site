package io.jokester.nuthatch.base

import com.google.gson.Gson
import io.circe.{Json, parser}
import io.jokester.api.OpenAPIConvention.ServerError

object QuillJsonHelper {
  private val gson = new Gson()
}

trait QuillJsonHelper {

  /** @deprecated
    *   only use on object that cannot have derived Json decoder/encoder
    * @param obj
    *   a object
    * @return
    */
  def toJson(obj: AnyRef): Json =
    parser
      .parse(QuillJsonHelper.gson.toJson(obj))
      .getOrElse(throw ServerError("error "))

  def fromJson[T](obj: Json, klass: Class[T]): T =
    QuillJsonHelper.gson.fromJson(obj.noSpaces, klass)
}
