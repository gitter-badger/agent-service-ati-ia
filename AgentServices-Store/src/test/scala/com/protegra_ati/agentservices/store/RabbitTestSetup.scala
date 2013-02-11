package com.protegra_ati.agentservices.store

trait RabbitTestSetup {
  val RABBIT_PORT_UI_PRIVATE = 5672
  val RABBIT_PORT_STORE_PRIVATE = 4000
  val RABBIT_PORT_STORE_DB = 5000
  val RABBIT_PORT_STORE_PUBLIC = 6000
  val RABBIT_PORT_STORE_PUBLIC_UNRELATED = 6001
  val RABBIT_PORT_VERIFIER = 6002
  val RABBIT_PORT_CLAIMING_AGENT = 6003
  val RABBIT_PORT_RELYING_AGENT = 6004
  val RABBIT_PORT_TEST_RESULTS_DB = 7000

} 