package net.navatwo.kaff4.verify

import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.rdf.Aff4Arn

internal sealed interface VerificationResult {
  val arn: Aff4Arn

  data class Success(override val arn: Aff4Arn) : VerificationResult

  data class Failure(
    override val arn: Aff4Arn,
    val failedHashes: Collection<VerifiableStreamProvider.Result.FailedHash>,
  ) : VerificationResult

  data class Unsupported(override val arn: Aff4Arn) : VerificationResult
}
