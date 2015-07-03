grammar Command;

@header {
import java.util.*;
}

command returns [Object[] c]:
	a=args { $c = $a.r.toArray(); }
;

args returns [List<Object> r]:
	a=arg { $r = new ArrayList<>(Arrays.asList($a.a)); }
|	l=args a=arg { $l.r.add($a.a); $r = $l.r; }
;

arg returns [Object a]:
	s=string { $a = $s.s; }
|	'[' c=command ']' { $a = $c.c; }
;

string returns [String s]:
	qs=QUOTED_STRING { $s = $qs.getText()
		.substring(1, $qs.getText().length()-1)
		.replaceAll("\\\\\\\\", "\\\\")
		.replaceAll("\\\\\"", "\"");
	}
|	nw=NON_WHITESPACE { $s = $nw.getText(); }
;

QUOTED_STRING:
	'"' (~('"' | '\\') | ('\\' ('\\' | '"')))* '"'
;

NON_WHITESPACE:
	(~(' ' | '\t' | '\n'))+
;

WHITESPACE:
	(' ' | '\t' | '\n') -> skip
;
