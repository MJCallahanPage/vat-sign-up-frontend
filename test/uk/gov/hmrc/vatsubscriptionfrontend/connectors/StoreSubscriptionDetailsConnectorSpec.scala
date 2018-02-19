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

package uk.gov.hmrc.vatsubscriptionfrontend.connectors

import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsubscriptionfrontend.config.AppConfig

class StoreSubscriptionDetailsConnectorSpec extends UnitSpec with MockitoSugar{

  val env = Environment.simple()
  val configuration = Configuration.load(env)


  object TestStoreSubscriptionDetailsConnector extends StoreSubscriptionDetailsConnector(
    mock[HttpClient],
    new AppConfig(configuration, env)
  )

  "The StoreSubscriptionDetailsConnector" should {

    "use the correct url" when {

      "storing the vat number" in {
        val testDetail = "vat-number"
        TestStoreSubscriptionDetailsConnector.storeSubscriptionDetailsUrl(testDetail) should endWith(s"/vat-subscription/subscription-request/$testDetail")
      }

    }
  }

}
