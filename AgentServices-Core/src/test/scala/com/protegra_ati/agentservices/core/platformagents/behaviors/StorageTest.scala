package com.protegra_ati.agentservices.core.platformagents.behaviors

import org.specs2.mutable._

import com.protegra.agentservicesstore.extensions.StringExtensions._
import com.protegra.agentservicesstore.extensions.URIExtensions._
import java.util.UUID
import com.protegra_ati.agentservices.core.schema._
import com.biosimilarity.lift.lib._
import com.protegra_ati.agentservices.core.messages.content._
import com.protegra.agentservicesstore.usage.AgentKVDBScope._
import com.protegra.agentservicesstore.usage.AgentKVDBScope.acT._
import com.protegra_ati.agentservices.core.schema._
import moniker._
import com.protegra_ati.agentservices.core.messages._
import com.protegra_ati.agentservices.core.schema.util._
import com.protegra_ati.agentservices.core._
import com.protegra_ati.agentservices.core.schema.disclosure._

import platformagents._
import scala.collection.JavaConversions._
import com.protegra_ati.agentservices.core.util.serializer.Serializer
import org.specs2.specification.Scope

trait StorageScope extends Scope
with Serializable
{

  val ProfileId = UUID.randomUUID
  val mockProfile = new Profile("FirstName", "LastName", "", "123456789@test.com", "CA", "someCAprovince", "city", "postalCode", "website")
  val basicProfile = new Profile("FirstName", "LastName", "", "", "CA", "", "", "", "")

  val JenId = ( "Jen" + UUID.randomUUID )
  val SteveId = ( "Steve" + UUID.randomUUID )

  val connSteve = ConnectionFactory.createConnection("Steve", ConnectionCategory.Person.toString, ConnectionCategory.Person.toString, "Basic", JenId, SteveId)

}

class StorageTest extends SpecificationWithJUnit
with RabbitTestSetup
with Timeouts
with SpecsPAHelpers
with Serializable
{
  val authorizedContentEmpty = ProfileDisclosedDataFactory.getDisclosedData(TrustLevel.Empty)
  val authorizedContentBasic = ProfileDisclosedDataFactory.getDisclosedData(TrustLevel.Basic)
  val authorizedContentFull = ProfileDisclosedDataFactory.getDisclosedData(TrustLevel.Full)

  val cnxnUIStore = new AgentCnxnProxy(( "UI" + UUID.randomUUID().toString ).toURI, "", ( "Store" + UUID.randomUUID().toString ).toURI)
  val pa = new AgentHostStorePlatformAgent
  AgentHostCombinedBase.setupStore(pa, cnxnUIStore)

  "updateData" should {

    "insert new data" in new StorageScope{
      var oldData: Profile = null
      pa.updateData(connSteve.writeCnxn, mockProfile.authorizedData(authorizedContentBasic.fields), oldData)
      val profileSearch: Profile = new Profile()
      Thread.sleep(TIMEOUT_MED)
      fetchMustBe(basicProfile)(pa, connSteve.writeCnxn, profileSearch.toSearchKey)
      countMustBe(1)(pa, connSteve.writeCnxn, profileSearch.toSearchKey)
    }

    "delete and insert existing data" in new StorageScope{
      val data = mockProfile.authorizedData(authorizedContentBasic.fields)
      pa.store(pa._dbQ, connSteve.writeCnxn, data.toStoreKey, Serializer.serialize[ Data ](data))
      Thread.sleep(TIMEOUT_MED)
      pa.updateData(connSteve.writeCnxn, data, data)
      val profileSearch: Profile = new Profile()

      fetchMustBe(basicProfile)(pa, connSteve.writeCnxn, profileSearch.toSearchKey)
      countMustBe(1)(pa, connSteve.writeCnxn, profileSearch.toSearchKey)
    }
  }
}