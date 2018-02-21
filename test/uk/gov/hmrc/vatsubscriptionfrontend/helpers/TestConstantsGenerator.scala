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

package uk.gov.hmrc.vatsubscriptionfrontend.helpers

import scala.util.Random


object TestConstantsGenerator {

  private val rand = new Random()

  private val UPPER_BOUND_9_DIGIT_NUMBER = 100000000
  private val UPPER_BOUND_8_DIGIT_NUMBER = 10000000
  // private val UPPER_BOUND_7_DIGIT_NUMBER = 1000000
  private val UPPER_BOUND_6_DIGIT_NUMBER = 100000

  def randomVatNumber: String = "%09d".format(rand.nextInt(UPPER_BOUND_9_DIGIT_NUMBER))

  def randomCrn: String = rand.nextInt(2) match {
    case 0 => randomCrnNumeric
    case 1 => randomCrnAlphaNumeric
    //case 2 => randomCrnNumericNoLeadingZeros
  }

  def randomCrnNumeric: String = "%08d".format(rand.nextInt(UPPER_BOUND_8_DIGIT_NUMBER))

  //todo not passing validation
  // def randomCrnNumericNoLeadingZeros: String = "%8d".format(rand.nextInt(UPPER_BOUND_7_DIGIT_NUMBER))

  // todo
  def randomCrnAlphaNumeric: String = "SC" + "%06d".format(rand.nextInt(UPPER_BOUND_6_DIGIT_NUMBER))

  private def randomString(alphabet: String)(max: Int): String =
    Stream.continually(rand.nextInt(alphabet.length)).map(alphabet).take(rand.nextInt(max) + 1).mkString

  private def randomAlphaNumericWithAdditional(additionalChars: String)(max: Int): String =
    randomString(('a' to 'z').mkString("") + ('A' to 'Z').mkString("") + ('0' to '9').mkString("") + additionalChars)(max)

  def randomEmail: String =
    randomAlphaNumericWithAdditional(additionalChars = "_.+-")(rand.nextInt(16) + 1) +
      "@" + randomAlphaNumericWithAdditional(additionalChars = "-")(rand.nextInt(10) + 1) +
      "." + {
      (0 to rand.nextInt(2)).map(_ => randomAlphaNumericWithAdditional(additionalChars = "-")(rand.nextInt(3) + 1)).mkString(".")
    }

}