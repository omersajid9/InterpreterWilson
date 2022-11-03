package wilson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import wilson.Expr.Assign;
import wilson.Expr.Binary;
import wilson.Expr.Call;
import wilson.Expr.Grouping;
import wilson.Expr.Literal;
import wilson.Expr.Logical;
import wilson.Expr.Unary;
import wilson.Expr.Variable;
import wilson.Stmt.Block;
import wilson.Stmt.Expression;
import wilson.Stmt.For;
import wilson.Stmt.Function;
import wilson.Stmt.If;
import wilson.Stmt.Print;
import wilson.Stmt.Return;
import wilson.Stmt.Var;
import wilson.Stmt.While;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>
{
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter)
    {
        this.interpreter = interpreter;
    }

    private enum FunctionType
    {
        NONE,
        FUNCTION
    }
    
    @Override
    public Void visitExpressionStmt(Expression stmt) 
    {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitForStmt(For stmt) 
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) 
    {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) 
    {
        for (Expr expr: stmt.expression) resolve(expr);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Function stmt) 
    {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Function function, FunctionType type)
    {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param: function.params)
        {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitVarStmt(Var stmt) 
    {
        declare(stmt.name);
        if (stmt.initializer != null)resolve(stmt.initializer);
        define(stmt.name);
        return null;
    }

    private void declare(Token name)
    {
        if (scopes.empty()) return;
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) Wilson.error(name, "Already a variable with this name in this scope.");
        scope.put(name.lexeme, false);
    }
    
    private void define(Token name)
    {
        if (scopes.empty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    @Override
    public Void visitBlockStmt(Block stmt) 
    {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    void resolve(List<Stmt> statements)
    {
        for (Stmt statement: statements) resolve(statement);
    }

    

    private void resolve(Stmt statement)
    {
        statement.accept(this);
    }

    private void resolve(Expr expression)
    {
        expression.accept(this);
    }

    private void beginScope()
    {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope()
    {
        scopes.pop();
    }

    @Override
    public Void visitWhileStmt(While stmt) 
    {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) 
    {
        if (currentFunction != FunctionType.FUNCTION) Wilson.error(stmt.keyword, "Cannot return from top-level code");
        if (stmt.value != null) resolve(stmt.value);
        return null;
    }

    @Override
    public Void visitAssignExpr(Assign expr) 
    {
        resolve(expr.value);
        resolveLocal(expr.value, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Binary expr) 
    {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Call expr) 
    {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) resolve(argument);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Grouping expr) 
    {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Literal expr) 
    {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Logical expr) 
    {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Unary expr) 
    {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Variable expr) 
    {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE)
        {
            Wilson.error(expr.name, "Cannot read local variable in its own initializer");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name)
    {
        for (int i = scopes.size() - 1; i >= 0; i--)
        {
            if (scopes.get(i).containsKey(name.lexeme))
            {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    
}

    
    