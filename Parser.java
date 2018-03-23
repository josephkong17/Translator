/* *** This file is given as part of the programming assignment. *** */
import java.util.*;



public class Parser {


    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    int numscope = 0; //we store the number we find after ~ here so we know what scope to check
    int i = 0; //counter variable
    String num; //string variable to hold numscope number converted into string
    ArrayList<ArrayList<String>> symtab = new ArrayList<ArrayList<String>>(); //initializes an arrayList of arrayLists
    private void scan() {
    tok = scanner.scan();
    }

    private Scan scanner;
    Parser(Scan scanner) {
    this.scanner = scanner;
    scan();
    program();
    if( tok.kind != TK.EOF )
        parse_error("junk after logical end of program");
    }

    private void program() {
    block();
    }

    private void block(){
    ArrayList<String> scope = new ArrayList<String>(); //everytime we enter block, we create a new block (arrayList)
    symtab.add(scope); //by adding the new arrayList into the arrayList of arrayLists
    declaration_list();
    statement_list();
    }

    private void declaration_list() {
    // below checks whether tok is in first set of declaration.
    // here, that's easy since there's only one token kind in the set.
    // in other places, though, there might be more.
    // so, you might want to write a general function to handle that.
    while( is(TK.DECLARE) ) {
        declaration();
    }
    }
    
    private void declaration() {
        mustbe(TK.DECLARE);
        scan();
        mustbe(TK.ID);
            if (symtab.get(symtab.size() - 1).contains(tok.string)) { //checks for redeclaration in current scope
            System.err.println("redeclaration of variable " + tok.string);
        }
        
        else {
            symtab.get(symtab.size() - 1).add(tok.string); //otherwise if it's not redeclared, add it to the list of variables declared
        }
        scan();
        
            while( is(TK.COMMA) ) {
                scan();
                mustbe(TK.ID);
                
                if (symtab.get(symtab.size() - 1).contains(tok.string)) { //checks for redeclaration in current scope
                    System.err.println("redeclaration of variable " + tok.string);
                }
                else {
                    symtab.get(symtab.size() - 1).add(tok.string); //otherwise if it's not redeclared, add it to the list of variables declared
                }
                scan();
            }
    }

    private void statement_list() {
        while( is(TK.TILDE) || is(TK.ID) || is(TK.PRINT) || is(TK.DO) || is(TK.IF) ) {
            statement();
        }
    }
    
    private void statement() {
        if ( is(TK.TILDE)) {
            assignment();
        }
        else if ( is(TK.PRINT)) {
            print();
        }
        else if ( is(TK.DO)) {
            _do();
        }
        else if ( is(TK.IF)) {
            _if();
        }
        else if ( is(TK.ID)) {
            assignment();
        }
    }
    
    private void print() {
        mustbe(TK.PRINT);
        scan();
        expr();
    }
    
    private void assignment() {
        ref_id();
        mustbe(TK.ASSIGN);
        scan();
        expr();
    }
    
    private void ref_id() {
        if (is(TK.TILDE)) {
            scan();
            if (is(TK.NUM)) {
                numscope = Integer.parseInt(tok.string); //if it finds a number after tilde, converts that number to an int and stores in numscope
                scan();
                mustbe(TK.ID);
                    if (numscope > (symtab.size() - 1)) { //if the number behind tilde is greater than the number of scopes we have, automatically no variable
                        System.err.println("no such variable ~" + numscope + tok.string + " on line " + tok.lineNumber);
                        System.exit(1);
                    }
                    
                    else if (symtab.get(symtab.size() - numscope - 1).contains(tok.string)) { //if our scope contains the variable, it's safe to use so we go on
                    }
                    else {
                        num = String.valueOf(numscope); //converts number behind tilde back to string so we can use it in the print error message
                        System.err.println("no such variable ~" + num + tok.string + " on line " + tok.lineNumber);
                        System.exit(1);
                    }
                scan();
            }
            else{
                mustbe(TK.ID);
                
                if (symtab.get(0).contains(tok.string)) { //checks for globally declared variables
                }
                
                else { //if it doesn't find it in the first scope where the globals are, it's undeclared
                     System.err.println("no such variable ~" + tok.string + " on line " + tok.lineNumber);
                    System.exit(1);
                }
                scan();
            }
        }

        else {
            mustbe(TK.ID);
            for (i = symtab.size() - 1; i >=0; i--) { //for loop that iterates backwards from most recent scope
                if (i >= 0 && symtab.get(i).contains(tok.string)) { //if it finds the variable in any of the scopes, it can be used
                    break;
                }

                else if (i == 0 && !symtab.get(i).contains(tok.string)) { //otherwise not in the scopes so undeclared
                    System.err.println(tok.string + " is an undeclared variable on line "  + tok.lineNumber);
                    System.exit(1);
                }
            }
            
            scan();
        }
    }
    
    private void _do() {
        mustbe(TK.DO);
        scan();
        guarded_command();
        mustbe(TK.ENDDO);
        symtab.remove(symtab.size() - 1);
        scan();
    }
    
    private void _if() {
        mustbe(TK.IF);
        scan();
        guarded_command();
        while ( is(TK.ELSEIF)) {
            scan();
            guarded_command();
        }
        if ( is(TK.ELSE)) {
            symtab.remove(symtab.size() - 1);
            scan();
            block();
        }
        mustbe(TK.ENDIF);
        scan();
        symtab.remove(symtab.size() - 1);
    }
    
    private void guarded_command() {
        expr();
        mustbe(TK.THEN);
        scan();
        block();
    }
    
    private void expr() {
        term();
        while ( is(TK.PLUS) || is(TK.MINUS)) {
            scan();
            term();
        }
    }
    
    private void term() {
        factor();
        while ( is(TK.TIMES) || is(TK.DIVIDE)) {
            scan();
            factor();
        }
    }
    
    private void factor() {
        if ( is(TK.LPAREN)) {
            scan();
            expr();
            mustbe(TK.RPAREN);
            scan();
        }
        else if ( is(TK.TILDE) || is(TK.ID)) {
            //scan();
            ref_id();
        }
        else {
            mustbe(TK.NUM);
            scan();
        }
    }
    
    private void addop() {
        if ( is(TK.PLUS)) {
            scan();
            mustbe(TK.PLUS);
        }
        else {
            mustbe(TK.MINUS);
            scan();
        }
    }
    
    private void multop() {
        if ( is(TK.TIMES)) {
            scan();
            mustbe(TK.TIMES);
            scan();
        }
        else {
            mustbe(TK.DIVIDE);
            scan();
        }
    }

    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
    if( tok.kind != tk ) {
        System.err.println( "mustbe: want " + tk + ", got " +
                    tok);
        parse_error( "missing token (mustbe)" );
    }
    //scan(); //takes out the scan from the mustbe so everytime we call mustbe it doesn't consume current variable
              // instead manually scan after every mustbe
    }

    private void parse_error(String msg) {
    System.err.println( "can't parse: line "
                + tok.lineNumber + " " + msg );
    System.exit(1);
    }
}