package filters

import javax.inject.{Inject, Singleton}

import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import com.mohiva.play.xmlcompressor.XMLCompressorFilter
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter

/**
  * Created by DongHee Kim on 2017-09-26 026.
  */
@Singleton
class CompressorFilters @Inject()(htmlCompressorFilter: HTMLCompressorFilter, xmlCompressorFilter: XMLCompressorFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(
    htmlCompressorFilter,
    xmlCompressorFilter
  )
}