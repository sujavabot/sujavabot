package org.sujavabot.core;

public abstract class CommandComponent {
	private CommandComponent() {}
	
	@Override
	public abstract String toString();
	
	public static class LiteralString extends CommandComponent {
		private String value;
		public LiteralString(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
		@Override
		public String toString() {
			return value;
		}
	}
	
	public static class Expression extends CommandComponent {
		private CommandComponent[] value;
		public Expression(CommandComponent[] value) {
			this.value = value;
		}
		public CommandComponent[] getValue() {
			return value;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			String sep = "[";
			for(CommandComponent cc : value) {
				sb.append(sep);
				sb.append(cc.toString());
				sep = " ";
			}
			sb.append("]");
			return sb.toString();
		}
	}
	public static class Quote extends CommandComponent {
		private CommandComponent value;
		public Quote(CommandComponent value) {
			this.value = value;
		}
		public CommandComponent getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "'" + value.toString();
		}
	}
	public static class QuasiQuote extends CommandComponent {
		private CommandComponent value;
		public QuasiQuote(CommandComponent value) {
			this.value = value;
		}
		public CommandComponent getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "`" + value.toString();
		}
	}
	public static class Unquote extends CommandComponent {
		private CommandComponent value;
		public Unquote(CommandComponent value) {
			this.value = value;
		}
		public CommandComponent getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "~" + value.toString();
		}
	}
	public static class UnquoteSplicing extends CommandComponent {
		private CommandComponent value;
		public UnquoteSplicing(CommandComponent value) {
			this.value = value;
		}
		public CommandComponent getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "~@" + value.toString();
		}
	}
}
