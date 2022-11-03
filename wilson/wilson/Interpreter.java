package wilson;

import java.util.ArrayList;
import java.util.HashMap;
// import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
// import wilson.Stmt.Return;
import wilson.Stmt.Var;
import wilson.Stmt.While;
import wilson.Return;


public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>
{
    final Environment globals = new Environment();
    private Environment env = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    void interpret(Expr expr)
    {
        try
        {
            Object value = evaluate(expr);
            System.out.println(stringify(value));
        }
        catch (RuntimeError error)
        {
            Wilson.runtimeError(error);
        }
    }

    void interpret(List<Stmt> statements)
    {
        try
        {
            for (Stmt statement: statements)
            {
                execute(statement);
            }
            
        }
        catch (RuntimeError error)
        {
            Wilson.runtimeError(error);
        }
    }

    private String stringify(Object object)
    {
        if (object == null) return "nil";
        if (object instanceof Double)
        {
            String text = object.toString();
            if (text.endsWith(".0"))
            {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }



    @Override
    public Object visitBinaryExpr(Binary expr) 
    {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type)
        {
            case MINUS:
            return (double) left - (double) right;
            case PLUS:
            if (left instanceof Double && right instanceof Double)
            {
                return (double) left + (double) right;
            }
            if (left instanceof String && right instanceof String) 
            {
                return (String)left + (String)right;
            }
            throw new RuntimeError(expr.operator, "Operands must be either numbers or strings");
            case SLASH:
            checkNumberOperands(expr.operator, left, right);
            return (double) left / (double) right;
            case STAR:
            checkNumberOperands(expr.operator, left, right);
            return (double) left * (double) right;
            case GREATER:
            checkNumberOperands(expr.operator, left, right);
            return (double) left > (double) right;
            case GREATER_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return (double) left >= (double) right;
            case LESS:
            checkNumberOperands(expr.operator, left, right);
            return (double) left < (double) right;
            case LESS_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return (double) left <= (double) right;
            case BANG_EQUAL:
            return !isEqual(left, right);
            case EQUAL_EQUAL:
            return isEqual(left, right);

        }
        return null;
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) 
    {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Literal expr) 
    {
       return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Unary expr) 
    {
        Object right = evaluate(expr.right);
        switch (expr.operator.type)
        {
            case MINUS:
            checkNumberOperand(expr.operator, right);
            return -(double)right;
            case BANG:
            return !isTruthy(right);

        }
        return null;
    }

    private void checkNumberOperands(Token token, Object left, Object right)
    {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(token, "Operands must be numbers.");
    }

    private void checkNumberOperand(Token token, Object right)
    {
        if (right instanceof Double) return;
        throw new RuntimeError(token, "Operand must be a number.");
    }

    private boolean isEqual(Object left, Object right)
    {
        if (left == null && right == null) return true;
        if (left == null) return false;
        return left.equals(right);
    }

    private boolean isTruthy(Object object)
    {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private Object evaluate(Expr expr)
    {
        return expr.accept(this);
    }

    private void execute(Stmt statement)
    {
        statement.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) 
    {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print expressions) 
    {
        for (Expr expression : expressions.expression)
        {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        }
        
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) 
    {
        Object value = null;
        if (stmt.initializer != null)
        {
            value = evaluate(stmt.initializer);
        }
        env.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitVariableExpr(Variable expr) 
    {
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(Token name, Expr expr)
    {
        Integer distance = locals.get(expr);
        if (distance != null) return env.getAt(distance, name.lexeme);
        else return globals.get(name);
    }

    @Override
    public Object visitAssignExpr(Assign expr) 
    {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) env.assignAt(distance, expr.name, value);
        else globals.assign(expr.name, value);
        return value;
    }

    @Override
    public Void visitBlockStmt(Block block) 
    {
        executeBlock(block.statements, new Environment(this.env));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment)
    {
        Environment previous = this.env;
        try
        {
            this.env = environment;
            for (Stmt statement : statements)
            {
    
                statement.accept(this);
            }
        }
        finally
        {
            this.env = previous;
        }
        
    }

    @Override
    public Void visitIfStmt(If stmt) 
    {
        if (isTruthy(evaluate(stmt.condition)))
        {
            execute(stmt.thenBranch);
        }
        else if (stmt.elseBranch != null)
        {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) 
    {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR)
        {
            if (isTruthy(left)) return left;
        }
        else
        {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Void visitWhileStmt(While stmt) 
    {
       while(isTruthy(evaluate(stmt.condition)))
       {
        execute(stmt.body);
       }
       return null;
    }

    @Override
    public Void visitForStmt(For stmt) 
    {
        return null;
    }

    @Override
    public Object visitCallExpr(Call expr) 
    {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument: expr.arguments)
        {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof WilsonCallable))
        {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        WilsonCallable function = (WilsonCallable) callee;

        if (arguments.size() != function.arity()) 
        {
            throw new RuntimeError(expr.paren, "Expected " +
                function.arity() + " arguments but got " +
                arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Void visitFunctionStmt(Function stmt) 
    {
        WilsonFunction function = new WilsonFunction(stmt, env);
        globals.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) 
    {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value);
    }

    void resolve(Expr expr, int depth)
    {
        locals.put(expr, depth);
    }

    
}
