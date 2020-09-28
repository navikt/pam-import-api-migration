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
abstract class AdStateRepository(val connection: Connection): CrudRepository<AdState, Long> {

    val insertSQL = """insert into "ad_state" ("uuid", "reference", "provider_id", "json_payload", "version_id", "created", "updated", "id") values (?,?,?,?,?,?,?,?)"""
    val updateSQL = """update "ad_state" set "uuid"=?,"reference"=?, "provider_id"=?, "json_payload"=?, "version_id"=?, "created"=?, "updated"=? where "id"=?"""

    @Transactional
    override fun <S : AdState> save(entity: S): S {
        if (!existsById(entity.id!!)) {
            connection.prepareStatement(insertSQL).apply {
                prepareSQL(entity)
                execute()
                @Suppress("UNCHECKED_CAST")
                return entity
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

    private fun PreparedStatement.prepareSQL(entity: AdState) {
        setString(1, entity.uuid)
        setString(2, entity.reference)
        setLong(3, entity.providerId)
        setString(4, entity.jsonPayload)
        setLong(5, entity.versionId)
        setTimestamp(6, entity.created.toTimeStamp())
        setTimestamp(7, entity.updated.toTimeStamp())
        setLong(8, entity.id!!)
    }

    @Transactional
    override fun <S : AdState> saveAll(entities: Iterable<S>): Iterable<S> {
        return entities.map { save(it) }.toList()
    }

    @Transactional
    abstract fun findByProviderIdAndReference(providerId: Long, reference: String): Optional<AdState>

    @Transactional
    abstract fun list(pageable: Pageable): Slice<AdState>

    @Transactional
    abstract fun findByUpdatedGreaterThanEquals(updated: LocalDateTime, pageable: Pageable): Slice<AdState>

    @Transactional
    abstract fun findByUuid(uuid: String): Optional<AdState>


    @Transactional
    abstract fun findByUuidAndProviderId(uuid: String, providerId: Long): Optional<AdState>

    @Transactional
    abstract fun list(versionId: Long, pageable: Pageable): Slice<AdState>

    @Transactional
    abstract fun list(versionId: Long, providerId: Long, pageable: Pageable): Slice<AdState>

}
