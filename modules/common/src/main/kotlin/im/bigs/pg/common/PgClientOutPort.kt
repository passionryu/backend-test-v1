package im.bigs.pg.common

import im.bigs.pg.common.PgApproveResult

/** 외부 결제사(PG) 승인 연동 포트. */
interface PgClientOutPort {
    fun supports(partnerId: Long): Boolean
    fun approve(request: PgApproveRequest): PgApproveResult
}
