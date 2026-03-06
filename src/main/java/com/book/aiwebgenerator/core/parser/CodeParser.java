package com.book.aiwebgenerator.core.parser;

public interface CodeParser<T> {
    T parseCode(String codeContent);
}
