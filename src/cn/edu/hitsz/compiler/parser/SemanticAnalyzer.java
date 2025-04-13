package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private Stack<SourceCodeType> typesStack=new Stack<SourceCodeType>();
    private Stack<Symbol> symbolsStack=new Stack<Symbol>();
    private SymbolTable symbolTable;
    @Override
    public void whenAccept(Status currentStatus) {
        // 什么都不用做
        return;
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        Token token;
        SourceCodeType type;
        Symbol headSymbol = new Symbol(production.head());
        switch (production.index()) {
            case 4://S->D id
                token = symbolsStack.pop().getToken();
                symbolsStack.pop();
                symbolsStack.push(headSymbol);
                typesStack.pop();
                type = typesStack.pop();
                typesStack.push(null);
                symbolTable.set(token.getText(), type);
                break;
            case 5://D->int
                symbolsStack.pop();
                symbolsStack.push(headSymbol);
        }
    }
    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        String type=currentToken.getKindId();
        symbolsStack.push(new Symbol(currentToken));
        if(type.equals("int"))
            typesStack.push(SourceCodeType.Int);
        else
            typesStack.push(null);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable=table;
    }
}

