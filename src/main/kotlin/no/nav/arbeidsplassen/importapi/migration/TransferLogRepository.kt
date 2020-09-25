package no.nav.arbeidsplassen.importapi.migration

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.LocalDateTime
import java.util.*
import javax.transaction.Transactional

@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TransferLogRepository(private val connection: Connection): CrudRepository<TransferLog, Long> {

    val insertSQL = """insert into "transfer_log" ("provider_id", "items", "md5", "payload", "status", "message", "created", "updated", "id") values (?,?,?,?,?,?,?,?,?)"""
    val updateSQL = """update "transfer_log" set "provider_id"=?, "items"=?, "md5"=?, "payload"=?, "status"=?, "message"=?, "created"=?, "updated"=? where "id"=?"""

    @Transactional
    override fun <S : TransferLog> save(entity: S): S {
        if (!existsById(entity.id!!)) {
            connection.prepareStatement(insertSQL).apply {
                prepareSQL(entity)
                execute()
                @Suppress("UNCHECKED_CAST")
                return entity.copy(id = generatedKeys.getLong(1)) as S
            }
        }
        else {
            connection.prepareStatement(updateSQL).apply {
                prepareSQL(entity)
                executeUpdate()
                return entity
            }
        }
    }

    @Transactional
    abstract fun existsByProviderIdAndMd5(providerId: Long, md5: String): Boolean

    @Transactional
    abstract fun findByIdAndProviderId(id: Long, providerId: Long): Optional<TransferLog>

    @Transactional
    abstract fun findByStatus(status: TransferLogStatus, pageable: Pageable): List<TransferLog>

    @Transactional
    abstract fun deleteByUpdatedBefore(updated: LocalDateTime)

    @Transactional
    abstract fun findByUpdatedGreaterThanEquals(updated: LocalDateTime, pageable: Pageable): Slice<TransferLog>

    @Transactional
    override fun <S : TransferLog> saveAll(entities: Iterable<S>): Iterable<S> {
        return entities.map { save(it) }.toList()
    }

    private fun PreparedStatement.prepareSQL(entity: TransferLog) {
        setObject(1, entity.providerId)
        setInt(2, entity.items)
        setString(3, entity.md5)
        setString(4, entity.payload)
        setString(5, entity.status.name)
        setString(6, entity.message)
        setTimestamp(7, entity.created.toTimeStamp())
        setTimestamp(8, entity.updated.toTimeStamp())
        setLong(9, entity.id!!)
    }
}
