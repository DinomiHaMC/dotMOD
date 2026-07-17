package com.dinomiha.dotmod.feature.inventorysearch.query;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QueryParser {
    private final QueryTokenizer tokenizer = new QueryTokenizer();

    public AndNode parse(String input) throws QueryParseException {
        List<QueryTokenizer.Token> tokens = tokenizer.tokenize(input);
        List<QueryNode> filters = new ArrayList<>();
        for (QueryTokenizer.Token token : tokens) {
            if (token.type() == QueryTokenizer.TokenType.CLAUSE) {
                filters.add(parseClause(token));
            }
        }
        return new AndNode(filters);
    }

    private static FilterNode parseClause(QueryTokenizer.Token token) throws QueryParseException {
        OperatorLocation location = operator(token.text(), token.start());
        if (location == null) {
            return textFilter(
                    FilterType.ALL_TEXT, ComparisonOperator.CONTAINS,
                    token.text(), token.start(), token.end(), token.start()
            );
        }
        String fieldText = token.text().substring(0, location.index).strip();
        FilterType type = FilterType.parse(normalize(fieldText));
        if (type == null) {
            if (location.symbol.equals(":") && !fieldText.isBlank()) {
                return textFilter(
                        FilterType.ALL_TEXT, ComparisonOperator.CONTAINS,
                        token.text(), token.start(), token.end(), token.start()
                );
            }
            throw new QueryParseException(
                    QueryError.UNKNOWN_FIELD, token.start(), token.start() + Math.max(1, fieldText.length())
            );
        }
        ComparisonOperator comparison = ComparisonOperator.parse(location.symbol);
        int valueOffset = location.index + location.symbol.length();
        String valueSegment = token.text().substring(valueOffset);
        int leadingWhitespace = 0;
        while (leadingWhitespace < valueSegment.length()
                && Character.isWhitespace(valueSegment.charAt(leadingWhitespace))) {
            leadingWhitespace++;
        }
        String rawValue = valueSegment.strip();
        int absoluteValueStart = token.start() + valueOffset + leadingWhitespace;
        if (rawValue.isEmpty()) {
            throw new QueryParseException(QueryError.EMPTY_VALUE, absoluteValueStart, token.end());
        }
        if (rawValue.charAt(0) != '"' && rawValue.charAt(0) != '\''
                && ":=!<>".indexOf(rawValue.charAt(0)) >= 0) {
            throw new QueryParseException(
                    QueryError.INVALID_OPERATOR, absoluteValueStart, absoluteValueStart + 1
            );
        }
        if (type.numeric()) {
            if (comparison == ComparisonOperator.CONTAINS) {
                comparison = ComparisonOperator.EQUALS;
            }
            String decoded = decode(rawValue, absoluteValueStart);
            if (type == FilterType.COUNT && decoded.endsWith("%")) {
                throw new QueryParseException(QueryError.INVALID_NUMBER, absoluteValueStart, token.end());
            }
            if (!decoded.matches("[0-9]+%?")) {
                throw new QueryParseException(QueryError.INVALID_NUMBER, absoluteValueStart, token.end());
            }
            try {
                int number = Integer.parseInt(decoded.endsWith("%")
                        ? decoded.substring(0, decoded.length() - 1) : decoded);
                if (number < 0 || type == FilterType.DURABILITY && number > 100) {
                    throw new QueryParseException(QueryError.NUMBER_OUT_OF_RANGE, absoluteValueStart, token.end());
                }
                return new FilterNode(type, comparison, null, number, token.start(), token.end());
            } catch (NumberFormatException exception) {
                throw new QueryParseException(QueryError.INVALID_NUMBER, absoluteValueStart, token.end());
            }
        }
        if (comparison != ComparisonOperator.CONTAINS
                && comparison != ComparisonOperator.EQUALS
                && comparison != ComparisonOperator.NOT_EQUALS) {
            throw new QueryParseException(
                    QueryError.INVALID_OPERATOR,
                    token.start() + location.index,
                    token.start() + location.index + location.symbol.length()
            );
        }
        return textFilter(type, comparison, rawValue, token.start(), token.end(), absoluteValueStart);
    }

    private static FilterNode textFilter(
            FilterType type,
            ComparisonOperator operator,
            String rawValue,
            int start,
            int end,
            int valueStart
    ) throws QueryParseException {
        String decoded = normalize(decode(rawValue.strip(), valueStart));
        if (decoded.isEmpty()) {
            throw new QueryParseException(QueryError.EMPTY_VALUE, start, end);
        }
        if (decoded.codePointCount(0, decoded.length()) > 256) {
            throw new QueryParseException(QueryError.QUERY_TOO_LONG, start, end);
        }
        return new FilterNode(type, operator, decoded, null, start, end);
    }

    private static OperatorLocation operator(String clause, int absoluteStart) throws QueryParseException {
        char quote = 0;
        boolean escaped = false;
        for (int index = 0; index < clause.length(); index++) {
            char current = clause.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
            } else if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                }
            } else if ((current == '"' || current == '\'') && isQuoteStart(clause, index)) {
                quote = current;
            } else if (current == ':' || current == '=' || current == '<' || current == '>' || current == '!') {
                String symbol = index + 1 < clause.length()
                        && (current == '!' || current == '<' || current == '>')
                        && clause.charAt(index + 1) == '='
                        ? clause.substring(index, index + 2)
                        : Character.toString(current);
                if (ComparisonOperator.parse(symbol) == null) {
                    throw new QueryParseException(
                            QueryError.INVALID_OPERATOR,
                            absoluteStart + index,
                            absoluteStart + index + symbol.length()
                    );
                }
                return new OperatorLocation(index, symbol);
            }
        }
        return null;
    }

    private static String decode(String raw, int sourceStart) throws QueryParseException {
        String value = raw;
        if (!value.isEmpty() && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
            char quote = value.charAt(0);
            int closing = findClosingQuote(value, quote);
            if (closing < 0) {
                throw new QueryParseException(QueryError.UNTERMINATED_QUOTE, sourceStart, sourceStart + value.length());
            }
            if (!value.substring(closing + 1).isBlank()) {
                throw new QueryParseException(
                        QueryError.TRAILING_INPUT, sourceStart + closing + 1, sourceStart + value.length()
                );
            }
            value = value.substring(1, closing);
        } else {
            for (int index = 0; index < value.length(); index++) {
                if ((value.charAt(index) == '"' || value.charAt(index) == '\'') && isQuoteStart(value, index)) {
                    throw new QueryParseException(
                            QueryError.TRAILING_INPUT, sourceStart + index, sourceStart + value.length()
                    );
                }
            }
        }
        StringBuilder decoded = new StringBuilder(value.length());
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                decoded.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else {
                decoded.append(current);
            }
        }
        return decoded.toString();
    }

    private static int findClosingQuote(String value, char quote) {
        boolean escaped = false;
        for (int index = 1; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == quote) {
                return index;
            }
        }
        return -1;
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isQuoteStart(String value, int index) {
        if (index == 0) {
            return true;
        }
        char previous = value.charAt(index - 1);
        return Character.isWhitespace(previous) || previous == ':' || previous == '='
                || previous == '<' || previous == '>' || previous == '!';
    }

    private record OperatorLocation(int index, String symbol) {
    }
}
