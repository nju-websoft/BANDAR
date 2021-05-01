package beans;

public class FullNode implements Comparable<FullNode>{
	private int id;
    private String s, p, o;
    private int s_id, p_id, o_id;
    private double visibility = 0, coverage = 0;
    
    public int flag = 0;

    private double score;
    
    public FullNode(){}
    
    public FullNode clone() {
    	FullNode node = new FullNode();
    	node.id = id;
    	node.s = s;
    	node.p = p;
    	node.o = o;
    	node.s_id = s_id;
    	node.p_id = p_id;
    	node.o_id = o_id;
    	node.visibility = visibility;
    	node.coverage = coverage;
    	node.score = score;
    	return node;
    }
    
    @Override
    public int compareTo(FullNode object) {
        if (this.score > (object.score))
            return -1;
        else if (this.score == object.score)
            return 0;
        else return 1;
    }
    public void setNodeID(int num) {
    	id = num;
    }
    
    public int getNodeID() {
    	return id;
    }
    
    public void setS(String s) {
		this.s = s;
	}
    
    public String getS() {
    	return s;
    }
    
    public void setsid(int s) {
		s_id = s;
	}
	
	public int getsid() {
		return s_id;
	}
	
	public void setP(String p) {
		this.p = p;
	}
    
    public String getP() {
    	return p;
    }
    
    public void setpid(int p) {
		p_id = p;
	}
	
	public int getpid() {
		return p_id;
	}
	
	public void setO(String o) {
		this.o = o;
	}
    
    public String getO() {
    	return o;
    }
    
    public void setoid(int o) {
		o_id = o;
	}
	
	public int getoid() {
		return o_id;
	}
    
    public void setVis(double num) {
    	visibility = num;
    }
    public double getVis() {
    	return visibility;
    }
    public void setCov(double num) {
    	coverage = num;
    }
    public double getCov() {
    	return coverage;
    }
    public void setScore(double num) {
    	score = num;
    }
    public double getScore() {
    	return score;
    }
}
