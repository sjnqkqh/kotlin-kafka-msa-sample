package msa.user.repository

import msa.user.model.VerificationCode
import msa.user.model.VerificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VerificationCodeRepository : JpaRepository<VerificationCode, Long> {
    fun findByEmailAndCodeAndType(email: String, code: String, type: VerificationType): Optional<VerificationCode>
    fun findByEmailAndTypeAndIsUsedFalse(email: String, type: VerificationType): List<VerificationCode>
    fun deleteByEmailAndType(email: String, type: VerificationType)
}