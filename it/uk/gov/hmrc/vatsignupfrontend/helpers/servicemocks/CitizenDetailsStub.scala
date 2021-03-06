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

package uk.gov.hmrc.vatsignupfrontend.helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.vatsignupfrontend.models.UserDetailsModel

object CitizenDetailsStub extends WireMockMethods {
  def sautrUri(sautr: String) = s"/citizen-details/sautr/$sautr/"
  def ninoUri(nino: String) = s"/citizen-details/nino/$nino/"

  def stubGetCitizenDetailsBySautr(sautr: String)(status: Int, response: UserDetailsModel): StubMapping =
    when(method = GET, uri = sautrUri(sautr))
      .thenReturn(status = status, body = UserDetailsModel.citizenDetailsWrites.writes(response))

  def stubGetCitizenDetailsByNino(nino: String)(status: Int, response: UserDetailsModel): StubMapping =
    when(method = GET, uri = ninoUri(nino))
      .thenReturn(status = status, body = UserDetailsModel.citizenDetailsWrites.writes(response))

}
