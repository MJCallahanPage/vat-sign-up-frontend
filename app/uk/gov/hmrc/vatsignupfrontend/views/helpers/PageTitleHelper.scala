/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatsignupfrontend.views.helpers

import play.api.data.Form
import play.api.i18n.Messages

object PageTitleHelper {

  val separator: String = " - "
  val errorPrefixKey: String = "error.title_prefix"
  val govukKey: String = "service_name.govuk"

  def formatTitle(form: Option[Form[_]], serviceName: String, title: String)(implicit messages: Messages): String = {
    errorPrefix(form) + Set(title, serviceName, messages(govukKey)).reduce(_ + separator + _)
  }

  private def errorPrefix(form: Option[Form[_]]) (implicit messages: Messages): String = {
    form match {
      case Some(form) => if (form.hasErrors) messages(errorPrefixKey) else ""
      case None => ""
    }
  }

}