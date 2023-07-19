package com.ynixt.sharedfinances.config.hibernatetypes

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class StringArrayType : UserType<Array<String>> {
    override fun equals(x: Array<String>?, y: Array<String>?): Boolean {
        return x?.contentEquals(y) ?: false
    }

    override fun hashCode(x: Array<String>): Int {
        return x.hashCode()
    }

    override fun getSqlType(): Int {
        return Types.ARRAY
    }

    override fun returnedClass(): Class<Array<String>> {
        return Array<String>::class.java
    }

    override fun nullSafeGet(
        rs: ResultSet,
        position: Int,
        session: SharedSessionContractImplementor,
        owner: Any?
    ): Array<String>? {
        val array = rs.getArray(position)
        return if (array != null) array.array as Array<String> else null
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun assemble(cached: Serializable?, owner: Any?): Array<String>? {
        return this.deepCopy(cached as Array<String>);
    }

    override fun disassemble(value: Array<String>?): Serializable {
        return deepCopy(value) as Array<String?>
    }

    override fun deepCopy(value: Array<String>?): Array<String>? {
        return value?.clone()
    }

    override fun nullSafeSet(
        st: PreparedStatement,
        value: Array<String>?,
        index: Int,
        session: SharedSessionContractImplementor
    ) {
        if (value != null) {
            val array = session.jdbcConnectionAccess.obtainConnection().createArrayOf("text", value)
            st.setArray(index, array)
        } else {
            st.setNull(index, Types.ARRAY)
        }
    }
}
