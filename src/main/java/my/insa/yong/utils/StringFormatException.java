package my.insa.yong.utils;

/**
 * thrown when an invalid sequence like \g is encountered while parsing a symbol
 */
public class StringFormatException extends Exception { static final long serialVersionUID =30101L;

  public StringFormatException() {
    super();
  }

  public StringFormatException(String mess) {
    super(mess);
  }

  public StringFormatException(String mess,Throwable cause) {
    super(mess,cause);
  }

  public StringFormatException(Throwable cause) {
    super(cause);
  }

}
