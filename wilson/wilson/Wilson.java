package wilson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;



public class Wilson 
{
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static Interpreter interpreter = new Interpreter();
    public static void main(String[] args) throws Exception 
    {
        if (args.length > 1)
        {
            System.out.println("Usage Wilson [script]");
            System.exit(64);
        }
        else if (args.length == 1)
        {
            runFile(args[0]);
        }
        else
        {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException
    {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException
    {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while(true)
        {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
        }
    }

    private static void run(String source)
    {
        errorReset();
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        // Expr expressions = parser.parse();
        List<Stmt> statements = parser.parse();

        if (hadError) return;
        System.out.println("source");
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if (hadError) return;
        System.out.println("source");
        interpreter.interpret(statements);
        System.out.println("source");
        // System.out.println(new ASTPrinter().print(expressions));

    }

    static void error(Token token, String errorMessage)
    {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", errorMessage);
          } else {
            report(token.line, " at '" + token.lexeme + "'", errorMessage);
          }
    }

    static void error(int line, String message)
    {
        report(line, "", message);
    }

    private static void report(int line, String where, String message)
    {
        System.err.println("[Line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void runtimeError(RuntimeError error)
    {
        System.err.println(error.getMessage() + "\n[Line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    static void errorReset()
    {
        hadError = false;
        hadRuntimeError = false;
    }
}
