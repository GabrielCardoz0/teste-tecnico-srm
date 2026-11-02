package com.srm.creditengine.infrastructure.persistence.settlement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.ReceivableType;

@Repository
public class SettlementStatementRepository {

	private static final String SELECT_COLUMNS = """
			SELECT s.id, s.settled_at, a.legal_name, a.cnpj, s.receivable_type, s.face_value,
			       s.present_value_brl, s.discount_brl, s.payment_currency, s.exchange_rate, s.payment_amount
			""";

	private static final String SELECT_COUNT = "SELECT count(*)\n";

	private static final String FROM = """
			FROM settlement s
			JOIN receivable r ON r.id = s.receivable_id
			JOIN assignor a ON a.id = r.assignor_id
			""";

	private static final String SETTLED_FROM = "s.settled_at >= :settledFrom";

	private static final String SETTLED_TO = "s.settled_at < :settledTo";

	private static final String ASSIGNOR = "r.assignor_id = :assignorId";

	private static final String PAYMENT_CURRENCY = "s.payment_currency = :paymentCurrency";

	private static final String ORDER_AND_PAGE = "\nORDER BY s.settled_at DESC, s.id DESC\nLIMIT :limit OFFSET :offset";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public SettlementStatementRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Page<SettlementStatementRow> find(SettlementStatementFilter filter, Pageable pageable) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String fromWhere = FROM + where(filter, parameters);

		long total = jdbcTemplate.queryForObject(SELECT_COUNT + fromWhere, parameters, Long.class);
		if (total == 0) {
			return new PageImpl<>(List.of(), pageable, 0);
		}

		parameters.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
		List<SettlementStatementRow> rows = jdbcTemplate.query(SELECT_COLUMNS + fromWhere + ORDER_AND_PAGE,
				parameters, SettlementStatementRepository::mapRow);
		return new PageImpl<>(rows, pageable, total);
	}

	private static String where(SettlementStatementFilter filter, MapSqlParameterSource parameters) {
		List<String> conditions = new ArrayList<>();
		if (filter.settledFromInclusive() != null) {
			conditions.add(SETTLED_FROM);
			parameters.addValue("settledFrom", OffsetDateTime.ofInstant(filter.settledFromInclusive(), ZoneOffset.UTC));
		}
		if (filter.settledToExclusive() != null) {
			conditions.add(SETTLED_TO);
			parameters.addValue("settledTo", OffsetDateTime.ofInstant(filter.settledToExclusive(), ZoneOffset.UTC));
		}
		if (filter.assignorId() != null) {
			conditions.add(ASSIGNOR);
			parameters.addValue("assignorId", filter.assignorId());
		}
		if (filter.paymentCurrency() != null) {
			conditions.add(PAYMENT_CURRENCY);
			parameters.addValue("paymentCurrency", filter.paymentCurrency().name());
		}
		return conditions.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", conditions);
	}

	private static SettlementStatementRow mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new SettlementStatementRow(
				rs.getLong("id"),
				rs.getObject("settled_at", OffsetDateTime.class).toInstant(),
				rs.getString("legal_name"),
				rs.getString("cnpj"),
				ReceivableType.valueOf(rs.getString("receivable_type")),
				rs.getBigDecimal("face_value"),
				rs.getBigDecimal("present_value_brl"),
				rs.getBigDecimal("discount_brl"),
				PaymentCurrency.valueOf(rs.getString("payment_currency")),
				rs.getBigDecimal("exchange_rate"),
				rs.getBigDecimal("payment_amount"));
	}

}
