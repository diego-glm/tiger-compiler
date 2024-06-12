package ErrorMsg;

public class ParsingException extends Exception {

    ParsingException(String unknownInput) {
        super(unknownInput);
    }

}