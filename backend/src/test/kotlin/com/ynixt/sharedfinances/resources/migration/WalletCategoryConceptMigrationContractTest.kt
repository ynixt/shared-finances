package com.ynixt.sharedfinances.resources.migration

import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class WalletCategoryConceptMigrationContractTest {
    @Test
    fun `V46 migration should seed all predefined concepts exactly once`() {
        val seededBlock =
            requireNotNull(SEED_BLOCK_REGEX.find(sqlText())) {
                "Could not locate predefined concept seed block in V46."
            }.groupValues[1]
        val seededCodes = CODE_REGEX.findAll(seededBlock).map { match -> match.groupValues[1] }.toList()
        val duplicates = seededCodes.groupingBy { it }.eachCount().filterValues { count -> count > 1 }

        assertThat(seededCodes).containsExactlyInAnyOrderElementsOf(WalletCategoryConceptCode.entries.map { it.name })
        assertThat(duplicates).isEmpty()
    }

    @Test
    fun `V46 migration should define backfill and DEBT_SF reconciliation steps`() {
        val normalized = normalizedSql()

        assertThat(normalized)
            .contains("update wallet_entry_category cat set concept_id = concept.id")
            .contains("and lower(trim(cat.name::text)) = m.normalized_name")
            .contains("where cat.concept_id is null")
            .contains("insert into wallet_category_concept (id, kind, code, display_name) select unmatched.generated_concept_id, 'custom'")
            .contains("with debt_concept as")
            .contains("missing_users as")
            .contains("missing_groups as")
            .contains("'debt_sf'")
    }

    @Test
    fun `V46 migration should enforce owner-scope uniqueness for concept binding`() {
        val normalized = normalizedSql()

        assertThat(normalized)
            .contains(
                "create unique index idx_wallet_entry_category_user_id_concept_id on wallet_entry_category(user_id, concept_id) where user_id is not null",
            ).contains(
                "create unique index idx_wallet_entry_category_group_id_concept_id on wallet_entry_category(group_id, concept_id) where group_id is not null",
            ).contains("alter table wallet_entry_category alter column concept_id set not null")
    }

    private fun sqlText(): String =
        Files.readString(
            Path.of(
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V46__WalletCategoryConceptsAndDebtSfInvariant.sql",
            ),
        )

    private fun normalizedSql(): String = sqlText().lowercase().replace(Regex("\\s+"), " ")

    private companion object {
        val SEED_BLOCK_REGEX =
            Regex(
                "INSERT INTO wallet_category_concept \\(id, kind, code, display_name\\).*?FROM \\(\\s*VALUES(.*?)\\) AS seeded\\(concept_code\\);",
                setOf(RegexOption.DOT_MATCHES_ALL),
            )
        val CODE_REGEX = Regex("'([A-Z_]+)'")
    }
}
