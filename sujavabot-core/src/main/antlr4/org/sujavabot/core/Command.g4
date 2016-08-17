grammar Command;

@header {
//import java.util.*;
}

command returns [CommandComponent.Expression c]:
	a=args { 
		$c = new CommandComponent.Expression($a.r.toArray(new Object[$a.r.size()])); 
	}
;

args returns [List<Object> r]:
	a=arg { $r = new ArrayList<>(java.util.Arrays.asList($a.a)); }
|	l=args a=arg { $l.r.add($a.a); $r = $l.r; }
;

arg returns [Object a]:
	s=string { $a = $s.s; }
	'\'' sa=arg { $a = new CommandComponent.Quote($sa.a); }
	'`' sa=arg { $a = new CommandComponent.QuasiQuote($sa.a); }
	'~' sa=arg { $a = new CommandComponent.Unquote($sa.a); }
	'~@' sa=arg { $a = new CommandComponent.UnquoteSplicing($sa.a); }
|	'[' c=command ']' { $a = $c.c; }
;

string returns [String s]:
	qs=QUOTED_STRING { $s = $qs.getText()
		.substring(1, $qs.getText().length()-1)
		.replaceAll("\\\\\\\\", "\\\\")
		.replaceAll("\\\\(.)", "$1");
	}
|	nw=IDENTIFIER { $s = $nw.getText(); }
;

QUOTED_STRING:
	'"' (~('"' | '\\') | ('\\' . ))* '"'
;

IDENTIFIER:
	(~(' ' | '\t' | '\n' | '[' | ']' | '\'' | '`' | '~' | '@'))+
;

WHITESPACE:
	(' ' | '\t' | '\n') -> skip
;
