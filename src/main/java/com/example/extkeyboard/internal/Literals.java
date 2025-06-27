package com.example.extkeyboard.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vijha on 10/10/2017.
 */

public class Literals {
    private List<LiteralNode> list = new ArrayList<>();

    public void append(int code){

        list.add(new LiteralNode(new String(Character.toChars(code)), null));
    }

    public void insert(int position, int code){
        list.add(position, new LiteralNode(new String(Character.toChars(code)), null));
    }

    public void append(String text){
        for(int i = 0; i< text.length() ; i++){
            insert(i, text.codePointAt(i));
        }
    }

    public void replace (int start, int end, int code){
        LiteralNode node = list.get(start);
        for(int i = start + 1; i <= end && i < list.size(); i++){
            LiteralNode next = list.get(i);
            node.setSource(node.getSource() + next.getSource());
            next.setSource(null);
        }
        node.setTarget(new String(Character.toChars(code)));

        clean();

    }

    public void remove(int start, int end){
        if(list.size() > 0){
            for(int i = start; i < end && i < list.size(); i++){
                LiteralNode ln = list.get(i);
                ln.setSource(null);
            }
            clean();
        }

    }

    public void clear(){
        list.clear();
    }

    public String getSource(){
        StringBuilder sb = new StringBuilder();
        for(LiteralNode node : list){
            if(node.getSource() != null){
                sb.append(node.getSource());
            }
        }
        return  sb.toString();
    }

    private void clean(){
        List<LiteralNode> deleteNodes = new ArrayList<>();
        for(int i = 0; i < list.size(); i++){
            LiteralNode ln = list.get(i);
            if(ln.getSource() == null){
                deleteNodes.add(ln);
            }
        }
        list.removeAll(deleteNodes);
    }

    public int size(){
        return  list.size();
    }



    private class LiteralNode {
        private String source, target;

        public LiteralNode(String source, String target) {
            this.source = source;
            this.target = target;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
