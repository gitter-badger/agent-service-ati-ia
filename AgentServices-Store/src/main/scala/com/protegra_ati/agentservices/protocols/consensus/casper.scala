// -*- mode: Scala;-*- 
// Filename:    casper.scala 
// Authors:     lgm                                                    
// Creation:    Thu Jul 23 07:05:15 2015 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.synereo.casper

import scala.collection.mutable.Map
import scala.collection.mutable.MapProxy

import java.util.Date

trait BetT[Address,Hash] {
  def validator : Address
  def height : Int
  def blockHash : Hash
  def round : Int  
  def prob : Double 
  def timestamp : Date
}

case class Bet[Address,Hash](
  override val validator : Address,
  override val height : Int,
  override val blockHash : Hash,
  override val round : Int,
  override val prob : Double,
  override val timestamp : Date
) extends BetT[Address,Hash]

trait ConsensusDataT[Address,Data,Hash,Signature]
trait UnsignedBlockT[Address,Data,Hash,Signature] extends ConsensusDataT[Address,Data,Hash,Signature] {
  def height : Int
  def previousBlockHash : Option[Hash]
  def timeStamp : Date
  def ghostEntries : Seq[GhostT[Address,Data,Hash,Signature]]
  def feeDistribution : Option[FeeDistributionT[Address,Data,Hash,Signature]]
  def pruning : Option[PruneGhostTableT[Address,Data,Hash,Signature]]
  def bondUnbond : Seq[BondStatusT[Address,Data,Hash,Signature] with EntryT[Address,Data,Hash,Signature]]
  def reorgEntries : ReorgT[Address,Data,Hash,Signature]
  def txns : Seq[TxnT[Address,Data,Hash,Signature]]
  def proposer : Address
}
trait BlockT[Address,Data,Hash,Signature] extends UnsignedBlockT[Address,Data,Hash,Signature] {
  def unsignedBlock : UnsignedBlockT[Address,Data,Hash,Signature]
  def signature : Signature
}

trait BlockStatusT[Address,Data,Hash,Signature] {
  def block : BlockT[Address,Data,Hash,Signature]
  def validationStatus : Option[Boolean]
  def haveDependencies : Option[Boolean]
  def timeStamp : Date
}

trait ValidationT[Address,Data,Hash,Signature]
     extends ConsensusDataT[Address,Data,Hash,Signature]
{
  def bets : List[Bet[Address,Hash]]
  def signature : Signature
}

case class Validation[Address,Data,Hash,Signature](
  override val bets : List[Bet[Address,Hash]],
  override val signature : Signature
) extends ValidationT[Address,Data,Hash,Signature]
 
trait EvidenceT[Address,Data,Hash,Signature]
     extends ConsensusDataT[Address,Data,Hash,Signature]
{
  def address : Address
  def validations : List[ValidationT[Address,Data,Hash,Signature]]
}

case class Evidence[Address,Data,Hash,Signature](
  override val address : Address,
  override val validations : List[ValidationT[Address,Data,Hash,Signature]]
) extends EvidenceT[Address,Data,Hash,Signature]

case class UnsignedBlock[Address,Data,Hash,Signature](
  override val height : Int,
  override val previousBlockHash : Option[Hash],
  override val timeStamp : Date,
  override val ghostEntries : Seq[GhostT[Address,Data,Hash,Signature]],
  override val feeDistribution : Option[FeeDistributionT[Address,Data,Hash,Signature]],
  override val pruning : Option[PruneGhostTableT[Address,Data,Hash,Signature]],
  override val bondUnbond : Seq[BondStatusT[Address,Data,Hash,Signature] with EntryT[Address,Data,Hash,Signature]],
  override val reorgEntries : ReorgT[Address,Data,Hash,Signature],
  override val txns : Seq[TxnT[Address,Data,Hash,Signature]],
  override val proposer : Address
) extends UnsignedBlockT[Address,Data,Hash,Signature]

class Block[Address,Data,Hash,Signature](
  override val unsignedBlock : UnsignedBlockT[Address,Data,Hash,Signature],
  override val signature : Signature
) extends BlockT[Address,Data,Hash,Signature] {
  override def height : Int = unsignedBlock.height
  override def previousBlockHash : Option[Hash] = unsignedBlock.previousBlockHash
  override def timeStamp : Date = unsignedBlock.timeStamp
  override def ghostEntries : Seq[GhostT[Address,Data,Hash,Signature]]
  =
    unsignedBlock.ghostEntries
  override def feeDistribution : Option[FeeDistributionT[Address,Data,Hash,Signature]] = unsignedBlock.feeDistribution
  override def pruning : Option[PruneGhostTableT[Address,Data,Hash,Signature]] = unsignedBlock.pruning
  override def bondUnbond : Seq[BondStatusT[Address,Data,Hash,Signature] with
  EntryT[Address,Data,Hash,Signature]] = unsignedBlock.bondUnbond
  override def reorgEntries : ReorgT[Address,Data,Hash,Signature] =
    unsignedBlock.reorgEntries
  override def txns : Seq[TxnT[Address,Data,Hash,Signature]] = 
    unsignedBlock.txns
  override def proposer : Address = unsignedBlock.proposer
}

object Block {
  def apply[Address,Data,Hash,Signature](
    height : Int,
    previousBlockHash : Option[Hash],
    timeStamp : Date,
    ghostEntries : Seq[GhostT[Address,Data,Hash,Signature]],
    feeDistribution : Option[FeeDistributionT[Address,Data,Hash,Signature]],
    pruning : Option[PruneGhostTableT[Address,Data,Hash,Signature]],
    bondUnbond : Seq[BondStatusT[Address,Data,Hash,Signature] with EntryT[Address,Data,Hash,Signature]],
    reorgEntries : ReorgT[Address,Data,Hash,Signature],
    txns : Seq[TxnT[Address,Data,Hash,Signature]],
    proposer : Address,
    signature : Signature
  ) : Block[Address,Data,Hash,Signature] =
    new Block(
      new UnsignedBlock( 
	height,
	previousBlockHash,
	timeStamp,
	ghostEntries,
	feeDistribution,
	pruning,
	bondUnbond,
	reorgEntries,
	txns,
	proposer
      ),
      signature
    )
  def unapply[Address,Data,Hash,Signature](
    block : Block[Address,Data,Hash,Signature] 
  ) : Option[
    ( Int, Option[Hash], Date, Seq[GhostT[Address,Data,Hash,Signature]],
      Option[FeeDistributionT[Address,Data,Hash,Signature]],
      Option[PruneGhostTableT[Address,Data,Hash,Signature]],
      Seq[BondStatusT[Address,Data,Hash,Signature] with EntryT[Address,Data,Hash,Signature]],
      ReorgT[Address,Data,Hash,Signature],
    Seq[TxnT[Address,Data,Hash,Signature]], Address, Signature )
  ] = {
    Some(
      (
	block.unsignedBlock.height,
	block.unsignedBlock.previousBlockHash,
	block.unsignedBlock.timeStamp,
	block.unsignedBlock.ghostEntries,
	block.unsignedBlock.feeDistribution,
	block.unsignedBlock.pruning,
	block.unsignedBlock.bondUnbond,
	block.unsignedBlock.reorgEntries,
	block.unsignedBlock.txns,
	block.unsignedBlock.proposer,
	block.signature
      )
    )
  }
}

case class BlockStatus[Address,Data,Hash,Signature](
  override val block : BlockT[Address,Data,Hash,Signature],
  override val validationStatus : Option[Boolean],
  override val haveDependencies : Option[Boolean],
  override val timeStamp : Date
) extends BlockStatusT[Address,Data,Hash,Signature]

trait EntryT[Address,Data,Hash,Signature] {
  def prev : Hash
  def post : Hash
}

trait PaidServiceT {
  def fee : Int
}

case class TxnPayLoad[Address,Data,Signature](
  sender : Address,
  receiver : Address,
  data : Data,
  override val fee : Int,
  signature : Signature  
) extends PaidServiceT

trait GhostT[Address,Data,Hash,Signature] extends EntryT[Address,Data,Hash,Signature]
trait ReorgT[Address,Data,Hash,Signature]
     extends EntryT[Address,Data,Hash,Signature] {
  def txns : Seq[TxnT[Address,Data,Hash,Signature]]
}
trait TxnT[Address,Data,Hash,Signature]
     extends EntryT[Address,Data,Hash,Signature] with PaidServiceT {
       def payload : TxnPayLoad[Address,Data,Signature]
       def sender : Address = payload.sender
       override def fee : Int = payload.fee
}

case class Ghost[Address,Data,Hash,Signature](
  override val prev : Hash,
  consensusData : ConsensusDataT[Address,Data,Hash,Signature],
  override val post : Hash
) extends GhostT[Address,Data,Hash,Signature]

case class Reorg[Address,Data,Hash,Signature](
  override val prev : Hash,
  override val txns : Seq[TxnT[Address,Data,Hash,Signature]],
  override val post : Hash
) extends ReorgT[Address,Data,Hash,Signature]

case class Txn[Address,Data,Hash,Signature](
  override val prev : Hash,
  override val payload : TxnPayLoad[Address,Data,Signature],
  override val sender : Address,
  override val fee : Int,
  override val post : Hash
) extends TxnT[Address,Data,Hash,Signature] 

trait BondPayLoadT[Address,Signature]
 extends PaidServiceT {
   def validator : Address
   def bonder : Address 
   def bond : Int 
   def bondPeriod : Int 
   def signature : Signature
 }

trait UnbondPayLoadT[Address,Signature] {
  def validator : Address
  def bondPeriod : Int 
  def bondWithdrawal : Option[Int] 
  def signature : Signature
}

case class BondPayLoad[Address,Signature](
  override val validator : Address,
  override val bonder : Address,
  override val bond : Int,
  override val bondPeriod : Int,
  override val fee : Int,
  override val signature : Signature  
) extends BondPayLoadT[Address,Signature] 

case class UnbondPayLoad[Address,Signature](
  override val validator : Address,
  override val bondPeriod : Int,
  override val bondWithdrawal : Option[Int],
  override val signature : Signature
) extends UnbondPayLoadT[Address,Signature] 

trait BondStatusT[Address,Data,Hash,Signature]
trait BondT[Address,Data,Hash,Signature]
     extends EntryT[Address,Data,Hash,Signature]
     with BondStatusT[Address,Data,Hash,Signature] {
       def bondPayLoad : BondPayLoadT[Address,Signature]
}  
trait UnbondT[Address,Data,Hash,Signature]
     extends EntryT[Address,Data,Hash,Signature]
     with BondStatusT[Address,Data,Hash,Signature] {
       def unbondPayLoad : UnbondPayLoadT[Address,Signature]
}  

case class Bond[Address,Data,Hash,Signature](
  override val prev : Hash,
  override val bondPayLoad : BondPayLoadT[Address,Signature],
  override val post : Hash
) extends BondT[Address,Data,Hash,Signature]

case class Unbond[Address,Data,Hash,Signature](
  override val prev : Hash,
  override val unbondPayLoad : UnbondPayLoadT[Address,Signature],
  override val post : Hash
) extends UnbondT[Address,Data,Hash,Signature] 

trait FeeDistributionT[Address,Data,Hash,Signature]
     extends EntryT[Address,Data,Hash,Signature] {
}

trait PruneGhostTableT[Address,Data,Hash,Signature]
     extends EntryT[Address,Data,Hash,Signature] {
} 

case class FeeDistribution[Address,Data,Hash,Signature](
  override val prev : Hash,
  override val post : Hash
) extends FeeDistributionT[Address,Data,Hash,Signature]

case class PruneGhostTable[Address,Data,Hash,Signature](
  override val prev : Hash,
  override val post : Hash
) extends PruneGhostTableT[Address,Data,Hash,Signature]

trait GhostTableT[Address,Data,Hash,Signature]
extends MapProxy[Int,Map[BlockT[Address,Data,Hash,Signature],Seq[Bet[Address,Hash]]]]

case class GhostTable[Address,Data,Hash,Signature](
  override val self : Map[Int,Map[BlockT[Address,Data,Hash,Signature],Seq[Bet[Address,Hash]]]]
) extends GhostTableT[Address,Data,Hash,Signature]

trait SignatureOpsT[Address,Data,Hash,Signature] {
  def isValidSignature[Account](
    msgHash : Hash, sig : Signature, key : Account
  ) : Boolean
  
}