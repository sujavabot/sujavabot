package org.sujavabot.core.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public abstract class ConverterHelpers {

	public static class MarshalHelper {
		protected XStream x;
		protected HierarchicalStreamWriter writer;
		protected MarshallingContext context;
		
		protected List<Consumer<MarshalHelper>> handlers = new ArrayList<>();
		
		public MarshalHelper(XStream x, HierarchicalStreamWriter writer, MarshallingContext context) {
			this.x = x;
			this.writer = writer;
			this.context = context;
		}
		
		public XStream getX() {
			return x;
		}
		
		public HierarchicalStreamWriter getWriter() {
			return writer;
		}
		
		public MarshallingContext getContext() {
			return context;
		}
		
		public void field(String name, Supplier<?> value) {
			handlers.add(h -> {
				Object v = value.get();
				writer.startNode(name);
				if(v == null)
					writer.addAttribute("class", "null");
				else {
					writer.addAttribute("class", x.getMapper().serializedClass(v.getClass()));
					context.convertAnother(v);
				}
				writer.endNode();
			});
		}
		
		public <T> void field(String name, Class<T> type, Supplier<T> value) {
			handlers.add(h -> {
				Object v = value.get();
				writer.startNode(name);
				if(v == null)
					writer.addAttribute("class", "null");
				else
					context.convertAnother(v);
				writer.endNode();
			});
		}
		
		public <T> void field(String name, Supplier<T> value, Supplier<T> defaultValue) {
			handlers.add(h -> {
				Object v = value.get();
				Object dv = defaultValue.get();
				if(Objects.equals(v, dv))
					return;
				writer.startNode(name);
				if(v == null)
					writer.addAttribute("class", "null");
				else {
					writer.addAttribute("class", x.getMapper().serializedClass(v.getClass()));
					context.convertAnother(v);
				}
				writer.endNode();
			});
		}
		
		public <T> void field(String name, Class<T> type, Supplier<T> value, Supplier<T> defaultValue) {
			handlers.add(h -> {
				Object v = value.get();
				Object dv = defaultValue.get();
				if(Objects.equals(v, dv))
					return;
				writer.startNode(name);
				if(v == null)
					writer.addAttribute("class", "null");
				else
					context.convertAnother(v);
				writer.endNode();
			});
		}
		
		public void handler(String name, Consumer<MarshalHelper> handler) {
			handlers.add(h -> {
				writer.startNode(name);
				handler.accept(h);
				writer.endNode();
			});
		}
		
		public void write() {
			for(Consumer<MarshalHelper> h : handlers)
				h.accept(this);
		}
	}
	
	public static class UnmarshalHelper {
		protected XStream x;
		protected HierarchicalStreamReader reader;
		protected UnmarshallingContext context;
		
		protected Map<String, BiConsumer<Object, UnmarshalHelper>> handlers = new HashMap<>();
		
		public UnmarshalHelper(XStream x, HierarchicalStreamReader reader, UnmarshallingContext context) {
			this.x = x;
			this.reader = reader;
			this.context = context;
		}
		
		public XStream getX() {
			return x;
		}
		
		public HierarchicalStreamReader getReader() {
			return reader;
		}
		
		public UnmarshallingContext getContext() {
			return context;
		}
		
		public <T> void field(String name, Consumer<T> value) {
			handlers.put(name, (current, h) -> {
				T v;
				if("null".equals(reader.getAttribute("class")))
					v = null;
				else {
					@SuppressWarnings("unchecked")
					Class<T> type = x.getMapper().realClass(reader.getAttribute("class"));
					v = type.cast(context.convertAnother(current, type));
				}
				value.accept(v);
			});
		}
		
		public <T> void field(String name, Class<T> type, Consumer<T> value) {
			handlers.put(name, (current, h) -> {
				T v;
				if("null".equals(reader.getAttribute("class")))
					v = null;
				else {
					v = type.cast(context.convertAnother(current, type));
				}
				value.accept(v);
			});
		}
		
		public void handler(String name, BiConsumer<Object, UnmarshalHelper> handler) {
			handlers.put(name, handler);
		}
		
		public <T> T read(T current) {
			while(reader.hasMoreChildren()) {
				reader.moveDown();
				BiConsumer<Object, UnmarshalHelper> h = handlers.get(reader.getNodeName());
				if(h != null)
					h.accept(current, this);
				reader.moveUp();
			}
			return current;
		}
	}
	
	private ConverterHelpers() {}
}
