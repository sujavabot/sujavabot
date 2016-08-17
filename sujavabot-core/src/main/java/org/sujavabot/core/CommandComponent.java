package org.sujavabot.core;

public abstract class CommandComponent {
	private CommandComponent() {}
	
	@Override
	public abstract String toString();
	
	public static class Expression {
		private Object[] value;
		public Expression(Object[] value) {
			this.value = value;
		}
		public Object[] getValue() {
			return value;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			String sep = "[";
			for(Object cc : value) {
				sb.append(sep);
				sb.append(cc.toString());
				sep = " ";
			}
			sb.append("]");
			return sb.toString();
		}
	}
	public static class Quote {
		private Object value;
		public Quote(Object value) {
			this.value = value;
		}
		public Object getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "'" + value.toString();
		}
	}
	public static class QuasiQuote {
		private Object value;
		public QuasiQuote(Object value) {
			this.value = value;
		}
		public Object getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "`" + value.toString();
		}
	}
	public static class Unquote {
		private Object value;
		public Unquote(Object value) {
			this.value = value;
		}
		public Object getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "~" + value.toString();
		}
	}
	public static class UnquoteSplicing {
		private Object value;
		public UnquoteSplicing(Object value) {
			this.value = value;
		}
		public Object getValue() {
			return value;
		}
		@Override
		public String toString() {
			return "~@" + value.toString();
		}
	}
}
