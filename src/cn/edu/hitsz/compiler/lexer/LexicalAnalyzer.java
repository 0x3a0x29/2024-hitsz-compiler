package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.StreamSupport;

/**
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String readString;
    private List<Token> TokensList;
    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.readString=null;
        this.TokensList=new LinkedList<Token>();
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        Scanner scanner= null;
        try {
            scanner = new Scanner(new FileReader(path));
            if (scanner.hasNextLine()){
                readString=scanner.nextLine().concat(" ");
            }
            while (scanner.hasNextLine()){
                readString=readString.concat(scanner.nextLine()).concat(" ");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        readString=readString.replace('\t',' ');
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        String operaters="=+-*/();";
        String buffer="";
        String head;
        enum STATE{
            NOTHING,INT,RETURN,DIGIT_READ,ID_READ
        };
        STATE state=STATE.NOTHING;
        while (!readString.isEmpty()){
            switch(state){
                case NOTHING:
                    if (readString.startsWith(" ")){
                        readString=readString.substring(1);
                    }else if (readString.startsWith("int")){
                        state=STATE.INT;
                        buffer="int";
                        readString=readString.substring(3);
                    }else if (readString.startsWith("return")){
                        state=STATE.RETURN;
                        buffer="return";
                        readString=readString.substring(6);
                    }else if (Character.isDigit(readString.charAt(0))){
                        state=STATE.DIGIT_READ;
                    }else if (Character.isLetter(readString.charAt(0))){
                        state=STATE.ID_READ;
                    }else if (operaters.contains(readString.substring(0,1))){
                        if (readString.startsWith(";")){
                            TokensList.add(Token.simple("Semicolon"));
                        }else{
                            TokensList.add(Token.simple(readString.substring(0,1)));
                        }
                        readString = readString.substring(1);
                    }
                    else{
                        throw new RuntimeException("出现错误");
                    }
                    break;
                case INT:
                case RETURN:
                    head=readString.substring(0,1);
                    if (Character.isLetterOrDigit(readString.charAt(0))){
                        state=STATE.ID_READ;
                    }else if (head.equals(" ")){
                        state=STATE.NOTHING;
                        TokensList.add(Token.simple(buffer));
                        buffer="";
                    }else if (operaters.contains(head)){
                        TokensList.add(Token.simple(buffer));
                        buffer="";
                        if (head.equals(";")){
                            TokensList.add(Token.simple("Semicolon"));
                        }else{
                            TokensList.add(Token.simple(head));
                        }
                        state = STATE.NOTHING;
                    }
                    else{
                        throw new RuntimeException("出现错误");
                    }
                    readString = readString.substring(1);
                    break;
                case DIGIT_READ:
                    head=readString.substring(0,1);
                    if (Character.isDigit(readString.charAt(0))){
                        buffer=buffer.concat(head);
                    }else if (head.equals(" ")){
                        state=STATE.NOTHING;
                        TokensList.add(Token.normal("IntConst",buffer));
                        buffer="";
                    }else if (operaters.contains(head)){
                        TokensList.add(Token.normal("IntConst",buffer));
                        buffer="";
                        if (head.equals(";")){
                            TokensList.add(Token.simple("Semicolon"));
                        }else{
                            TokensList.add(Token.simple(head));
                        }
                        state = STATE.NOTHING;
                    }
                    else{
                        throw new RuntimeException("出现错误");
                    }
                    readString = readString.substring(1);
                    break;
                case ID_READ:
                    head=readString.substring(0,1);
                    if (Character.isLetterOrDigit(readString.charAt(0))){
                        buffer=buffer.concat(head);
                    }else if (head.equals(" ")){
                        state=STATE.NOTHING;
                        TokensList.add(Token.normal("id",buffer));
                        if (!symbolTable.has(buffer)){
                            symbolTable.add(buffer);
                        }
                        buffer="";
                    }else if (operaters.contains(head)){
                        TokensList.add(Token.normal("id",buffer));
                        if (!symbolTable.has(buffer)){
                            symbolTable.add(buffer);
                        }
                        buffer="";
                        if (head.equals(";")){
                            TokensList.add(Token.simple("Semicolon"));
                        }else{
                            TokensList.add(Token.simple(head));
                        }
                        state = STATE.NOTHING;
                    }
                    else{
                        throw new RuntimeException("出现错误");
                    }
                    readString = readString.substring(1);
                    break;
            }
        }
        switch (state){
            case ID_READ:
                TokensList.add(Token.normal("id",buffer));
                if (!symbolTable.has(buffer)){
                    symbolTable.add(buffer);
                }
                break;
            case DIGIT_READ:
                TokensList.add(Token.normal("IntConst",buffer));
                break;
            case INT:
            case RETURN:
                TokensList.add(Token.simple(buffer));
                break;
        }
        TokensList.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return TokensList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
