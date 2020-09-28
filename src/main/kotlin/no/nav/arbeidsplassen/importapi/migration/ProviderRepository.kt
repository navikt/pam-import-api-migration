package no.nav.arbeidsplassen.importapi.migration

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.transaction.Transactional


@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ProviderRepository(val connection:Connection): CrudRepository<Provider, Long> {

    val updateSQL = """update "provider" set "jwtid"=?, "identifier"=?, "email"=?, "phone"=?, "created"=?, "updated"=? where "id"=?"""
    val insertSQL = """insert into "provider" ("jwtid", "identifier", "email", "phone", "created", "updated", "id") values (?,?,?,?,?,?,?)"""

    @Transactional
    override fun <S : Provider> save(entity: S): S {
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

    @Transactional
    override fun <S : Provider> saveAll(entities: Iterable<S>): Iterable<S> {
        return entities.map { save(it) }.toList()
    }

    @Transactional
    abstract fun list(pageable: Pageable): Slice<Provider>

    private fun PreparedStatement.prepareSQL(entity: Provider) {
        setString(1, entity.jwtid)
        setString(2, entity.identifier)
        setString(3, entity.email)
        setString(4, entity.phone)
        setTimestamp(5, entity.created.toTimeStamp())
        setTimestamp(6, entity.updated.toTimeStamp())
        setObject(7, entity.id)
    }
}

fun LocalDateTime.toTimeStamp(): Timestamp {
    return Timestamp.valueOf(this)
}
