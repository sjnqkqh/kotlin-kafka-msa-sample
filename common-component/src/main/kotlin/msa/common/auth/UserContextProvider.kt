package msa.common.auth

/**
 * 현재 인증된 사용자의 컨텍스트를 제공하는 인터페이스
 *
 * 이 인터페이스를 통해 테스트 환경에서 Mock 구현체를 주입할 수 있어
 * 단위 테스트 시 실제 SecurityContext 없이도 인증 로직을 검증할 수 있습니다.
 */
interface UserContextProvider {

    /**
     * 현재 인증된 사용자의 컨텍스트를 반환합니다.
     *
     * @return 현재 사용자의 컨텍스트
     * @throws CustomException 인증되지 않은 경우 ACCESS_DENIED 에러 발생
     */
    fun getCurrentUser(): UserContext

    /**
     * 현재 인증된 사용자의 컨텍스트를 반환하거나, 인증되지 않은 경우 null을 반환합니다.
     *
     * @return 현재 사용자의 컨텍스트 또는 null
     */
    fun getCurrentUserOrNull(): UserContext?
}
