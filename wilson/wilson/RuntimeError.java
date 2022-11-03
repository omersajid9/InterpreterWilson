package wilson;

public class RuntimeError extends RuntimeException{
    final Token token;

    RuntimeError(Token token, String errorMessage)
    {
        super(errorMessage);
        this.token = token;
    }
    
}
