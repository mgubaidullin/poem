var JavaParser = Editor.Parser = (function() {
  var tokenizeJava = (function() {
    function normal(source, setState) {
      var ch = source.next();
      if (ch == "/" && source.equals("*")) {
        setState(inCComment);
        return null;
      }
      if(ch=="/" && source.equals("/")){
    	  setState(singleComment);
    	  return null;
      }
      else if (ch == "\"" || ch == "'") {
        setState(inString(ch));
        return null;
      }
      else if (/[;{}:\[\]]/.test(ch)) {
          return "java-punctuation";
        }
      else {
          source.nextWhileMatches(/[\w\\\-_]/);
          return "java-identifier";
      }
    }

    function inCComment(source, setState) {
      var maybeEnd = false;
      while (!source.endOfLine()) {
        var ch = source.next();
        if (maybeEnd && ch == "/") {
          setState(normal);
          break;
        }
        maybeEnd = (ch == "*");
      }
      return "java-comment";
    }
    
    function singleComment(source, setState){
    	while(!source.endOfLine()){
    		var ch = source.next();
    	}
    	setState(normal);
    	return "java-comment";
    }

    function inString(quote) {
      return function(source, setState) {
        var escaped = false;
        while (!source.endOfLine()) {
          var ch = source.next();
          if (ch == quote && !escaped)
            break;
          escaped = !escaped && ch == "\\";
        }
        if (!escaped)
          setState(normal);
        return "java-string";
      };
    }

    return function(source, startState) {
      return tokenizer(source, startState || normal);
    };
  })();

  function indentJava(inBraces, inRule, base) {
    return function(nextChars) {
      if (!inBraces || /^\}/.test(nextChars)) return base;
      else if (inRule) return base + indentUnit * 2;
      else return base + indentUnit;
    };
  }

  function parseJava(source, basecolumn) {
    basecolumn = basecolumn || 0;
    var tokens = tokenizeJava(source);
    var inBraces = false, inRule = false;

    var iter = {
      next: function() {
        var token = tokens.next(), style = token.style, content = token.content;

        if(content == "static" || content == "public" || content == "private" || content=="int" 
        	|| content=="float" || content=="boolean" || content=="true" || content=="false"
        		|| content=="package" || content=="import" || content=="class" || content=="return"
        			|| content=="null" || content=="double" || content=="try" || content=="while"
        				|| content=="for" || content=="do" || content=="if" || content=="else" || content=="void"){
        	token.style = "java-keyword";
        }
        
        if (content == "\n")
          token.indentation = indentJava(inBraces, inRule, basecolumn);

        if (content == "{")
          inBraces = true;
        else if (content == "}")
          inBraces = inRule = false;
        else if (inBraces && content == ";")
          inRule = false;
        else if (inBraces && style != "java-comment" && style != "whitespace")
          inRule = true;

        return token;
      },

      copy: function() {
        var _inBraces = inBraces, _inRule = inRule, _tokenState = tokens.state;
        return function(source) {
          tokens = tokenizeJava(source, _tokenState);
          inBraces = _inBraces;
          inRule = _inRule;
          return iter;
        };
      }
    };
    return iter;
  }

  return {make: parseJava, electricChars: "}"};
})();
