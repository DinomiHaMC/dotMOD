package com.dinomiha.dotmod.feature.inventorysearch.query;

import java.util.ArrayList;
import java.util.List;

/** Splits a bounded query into clauses while preserving source spans. */
public final class QueryTokenizer {
    public static final int MAX_LENGTH = 512;
    public static final int MAX_CLAUSES = 16;

    public List<Token> tokenize(String input) throws QueryParseException {
        String query = input == null ? "" : input;
        if (query.length() > MAX_LENGTH) {
            throw new QueryParseException(QueryError.QUERY_TOO_LONG, MAX_LENGTH, query.length());
        }
        List<Token> tokens = new ArrayList<>();
        int clauseStart = 0;
        char quote = 0;
        int quoteStart = -1;
        boolean escaped = false;
        for (int index = 0; index < query.length(); index++) {
            char current = query.charAt(index);
            if (escaped) {
                if (current != '\\' && current != '"' && current != '\'' && current != '&') {
                    throw new QueryParseException(QueryError.INVALID_ESCAPE, index - 1, index + 1);
                }
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                    quoteStart = -1;
                }
                continue;
            }
            if ((current == '"' || current == '\'') && isQuoteStart(query, index, clauseStart)) {
                quote = current;
                quoteStart = index;
            } else if (current == '&') {
                addClause(tokens, query, clauseStart, index);
                tokens.add(new Token(TokenType.AND, "&", index, index + 1));
                clauseStart = index + 1;
            }
        }
        if (escaped) {
            throw new QueryParseException(QueryError.INVALID_ESCAPE, query.length() - 1, query.length());
        }
        if (quote != 0) {
            throw new QueryParseException(QueryError.UNTERMINATED_QUOTE, quoteStart, query.length());
        }
        if (query.isBlank()) {
            return List.of(new Token(TokenType.EOF, "", query.length(), query.length()));
        }
        addClause(tokens, query, clauseStart, query.length());
        long clauses = tokens.stream().filter(token -> token.type == TokenType.CLAUSE).count();
        if (clauses > MAX_CLAUSES) {
            throw new QueryParseException(QueryError.TOO_MANY_CLAUSES, 0, query.length());
        }
        tokens.add(new Token(TokenType.EOF, "", query.length(), query.length()));
        return List.copyOf(tokens);
    }

    private static void addClause(List<Token> tokens, String query, int start, int end) throws QueryParseException {
        int first = start;
        int last = end;
        while (first < last && Character.isWhitespace(query.charAt(first))) {
            first++;
        }
        while (last > first && Character.isWhitespace(query.charAt(last - 1))) {
            last--;
        }
        if (first == last) {
            throw new QueryParseException(QueryError.EMPTY_CLAUSE, start, end);
        }
        tokens.add(new Token(TokenType.CLAUSE, query.substring(first, last), first, last));
    }

    private static boolean isQuoteStart(String query, int index, int clauseStart) {
        if (index == clauseStart) {
            return true;
        }
        char previous = query.charAt(index - 1);
        return Character.isWhitespace(previous) || previous == ':' || previous == '='
                || previous == '<' || previous == '>' || previous == '!';
    }

    public enum TokenType {
        CLAUSE,
        AND,
        EOF
    }

    public record Token(TokenType type, String text, int start, int end) {
    }
}
