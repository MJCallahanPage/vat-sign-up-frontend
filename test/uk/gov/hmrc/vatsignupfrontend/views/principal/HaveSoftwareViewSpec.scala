/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignupfrontend.views.principal

import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.vatsignupfrontend.assets.MessageLookup.{HaveSoftware => messages}
import uk.gov.hmrc.vatsignupfrontend.config.AppConfig
import uk.gov.hmrc.vatsignupfrontend.forms.HaveSoftwareForm._
import uk.gov.hmrc.vatsignupfrontend.views.ViewSpec


class HaveSoftwareViewSpec extends ViewSpec {

  val env = Environment.simple()
  val configuration = Configuration.load(env)
  val error = "error.have_software"

  lazy val messagesApi = app.injector.instanceOf[MessagesApi]

  lazy val page = uk.gov.hmrc.vatsignupfrontend.views.html.principal.have_software(
    haveSoftwareForm,
    postAction = testCall)(
    FakeRequest(),
    applicationMessages,
    new AppConfig(configuration, env)
  )

  "The Have Software view" should {

    val testPage = TestView(
      name = "Have Software View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )
    testPage.shouldHaveForm("Yes No Form")(actionCall = testCall)

    testPage.shouldHavePara(messages.line)

    "have a set of radio inputs" which {
      lazy val doc = Jsoup.parse(page.body)

      "for the option 'Yes'" should {

        "have the text 'yes'" in {
          doc.select(s"label[for=${messages.radioYes}]").text() shouldEqual messages.yes
        }

        "have an input under the label that" should {

          lazy val optionLabel = doc.select("#yes")

          "have the id 'yes'" in {
            optionLabel.attr("id") shouldEqual "yes"
          }

          "be of type radio" in {
            optionLabel.attr("type") shouldEqual "radio"
          }
        }
      }

      "for the option 'No'" should {

        "have the text 'no'" in {
          doc.select(s"label[for=${messages.radioNo}]").text() shouldEqual messages.no
        }

        "have an input under the label that" should {

          lazy val optionLabel = doc.select("#no")

          "have the id 'no" in {
            optionLabel.attr("id") shouldEqual "no"
          }

          "be of type radio" in {
            optionLabel.attr("type") shouldEqual "radio"
          }
        }
      }
    }

    testPage.shouldHaveContinueButton()
  }
}
