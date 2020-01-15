package branch_and_bound_vrptw;
import java.util.ArrayList;
import java.util.PriorityQueue;

import branch_and_bound_vrptw.Check;
import branch_and_bound_vrptw.Data;
import branch_and_bound_vrptw.Node;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
/**
 * @author： huangnan
 * @School: HuaZhong University of science and technology
 * @操作说明：读入不同的文件前要手动修改vetexnum参数，参数值为所有点个数,包括配送中心0和n+1，
 * 代码算例截取于Solomon测试算例
 *
 */
//类功能：建立模型并求解
public class BaB_Vrptw {
	Data data;					//定义类Data的对象
	Node node1;
	Node node2;
	int deep;//深度
	public PriorityQueue<Node> queue;//分支队列
	Node best_note;//当前最好分支
	double cur_best;//最好解
	int []record_arc;//记录需要分支的节点
	double x_gap;//很小的数
	IloCplex model;				//定义cplex内部类的对象		
	public IloNumVar[][][] x;	//x[i][j][k]表示弧arcs[i][j]被车辆k访问
	public IloNumVar[][] w;		//车辆访问所有点的时间矩阵
	double cost;				//目标值object
	double[][][] x_map;//cplex参数x
	ArrayList<ArrayList<Integer>> routes;		//定义车辆路径链表
	ArrayList<ArrayList<Double>> servetimes;	//定义花费时间链表
	
	public BaB_Vrptw(Data data) {
		this.data = data;
		x_gap = data.gap;
		routes = new ArrayList<>();		//定义车辆路径链表
		servetimes = new ArrayList<>();	//定义花费时间链表
		//初始化车辆路径和花费时间链表，链表长度为车辆数k
		for (int k = 0; k < data.veh_num; k++) {
			ArrayList<Integer> r = new ArrayList<>();	
			ArrayList<Double> t = new ArrayList<>();	
			routes.add(r);							
			servetimes.add(t);							
		}
		x_map = new double[data.vertex_num][data.vertex_num][data.veh_num];
	}
	public void clear_lp() {
		data=null;
		routes.clear();
		servetimes.clear();
		x_map=null;
	}
	//辅助lp解到node
	@SuppressWarnings("unchecked")
	public void copy_lp_to_node(BaB_Vrptw lp, Node node) {
		node.node_routes.clear();
		node.node_servetimes.clear();
		node.node_cost = lp.cost;
		for (int i = 0; i < lp.x_map.length; i++) {
			for (int j = 0; j < lp.x_map[i].length; j++) {
				node.lp_x[i][j] = lp.x_map[i][j].clone();
			}
		}
		for (int i = 0; i < lp.routes.size(); i++) {
			node.node_routes.add((ArrayList<Integer>) lp.routes.get(i).clone());
		}
		for (int i = 0; i < lp.servetimes.size(); i++) {
			node.node_servetimes.add((ArrayList<Double>) lp.servetimes.get(i).clone());
		}
	}
	//函数功能：根据VRPTW数学模型建立VRPTW的cplex模型
	//建立模型
	private void build_model() throws IloException {
		//model
		model = new IloCplex();
		model.setOut(null);
//		model.setParam(IloCplex.DoubleParam.EpOpt, 1e-9);
//		model.setParam(IloCplex.DoubleParam.EpGap, 1e-9);
		//variables
		x = new IloNumVar[data.vertex_num][data.vertex_num][data.veh_num];
		w = new IloNumVar[data.vertex_num][data.veh_num];				//车辆访问点的时间
		//定义cplex变量x和w的数据类型及取值范围
		for (int i = 0; i < data.vertex_num; i++) {
			for (int k = 0; k < data.veh_num; k++) {
		w[i][k] = model.numVar(0, 1e15, IloNumVarType.Float, "w" + i + "," + k);
			}
			for (int j = 0; j < data.vertex_num; j++) {
				if (data.arcs[i][j]==0) {
					x[i][j] = null;
				}
				else{
					//Xijk,公式(10)-(11)
					for (int k = 0; k < data.veh_num; k++) {
	x[i][j][k] = model.numVar(0, 1, IloNumVarType.Float, "x" + i + "," + j + "," + k);
					}
				}
			}
		}
		//加入目标函数
		//公式(1)
		IloNumExpr obj = model.numExpr();
		for(int i = 0; i < data.vertex_num; i++){
			for(int j = 0; j < data.vertex_num; j++){
				if (data.arcs[i][j]==0) {
					continue;
				}
				for(int k = 0; k < data.veh_num; k++){
					obj = model.sum(obj, model.prod(data.dist[i][j], x[i][j][k]));
				}
			}
		}
		model.addMinimize(obj);
		//加入约束
		//公式(2)
		for(int i= 1; i < data.vertex_num-1;i++){
			IloNumExpr expr1 = model.numExpr();
			for (int k = 0; k < data.veh_num; k++) {
				for (int j = 1; j < data.vertex_num; j++) {
					if (data.arcs[i][j]==1) {
						expr1 = model.sum(expr1, x[i][j][k]);
					}
				}
			}
			model.addEq(expr1, 1);
		}
		//公式(3)
		for (int k = 0; k < data.veh_num; k++) {
			IloNumExpr expr2 = model.numExpr();
			for (int j = 1; j < data.vertex_num; j++) {
				if (data.arcs[0][j]==1) {
					expr2 = model.sum(expr2, x[0][j][k]);
				}
			}
			model.addEq(expr2, 1);
		}
		//公式(4)
		for (int k = 0; k < data.veh_num; k++) {
			for (int j = 1; j < data.vertex_num-1; j++) {
				IloNumExpr expr3 = model.numExpr();
				IloNumExpr subExpr1 = model.numExpr();
				IloNumExpr subExpr2 = model.numExpr();
				for (int i = 0; i < data.vertex_num; i++) {
					if (data.arcs[i][j]==1) {
						subExpr1 = model.sum(subExpr1,x[i][j][k]);
					}
					if (data.arcs[j][i]==1) {
						subExpr2 = model.sum(subExpr2,x[j][i][k]);
					}
				}
				expr3 = model.sum(subExpr1,model.prod(-1, subExpr2));
				model.addEq(expr3, 0);
			}
		}
		//公式(5)
		for (int k = 0; k < data.veh_num; k++) {
			IloNumExpr expr4 = model.numExpr();
			for (int i = 0; i < data.vertex_num-1; i++) {
				if (data.arcs[i][data.vertex_num-1]==1) {
					expr4 = model.sum(expr4,x[i][data.vertex_num-1][k]);
				}
			}
			model.addEq(expr4, 1);
		}
		//公式(6)
		double M = 1e5;
		for (int k = 0; k < data.veh_num; k++) {
			for (int i = 0; i < data.vertex_num; i++) {
				for (int j = 0; j < data.vertex_num; j++) {
					if (data.arcs[i][j] == 1) {
						IloNumExpr expr5 = model.numExpr();
						IloNumExpr expr6 = model.numExpr();
						expr5 = model.sum(w[i][k], data.s[i]+data.dist[i][j]+2);//+2
						expr5 = model.sum(expr5,model.prod(-1, w[j][k]));
						expr6 = model.prod(M,model.sum(1,model.prod(-1, x[i][j][k])));
						model.addLe(expr5, expr6);
					}
				}
			}
		}
		//公式(7)
		for (int k = 0; k < data.veh_num; k++) {
			for (int i = 1; i < data.vertex_num-1; i++) {
				IloNumExpr expr7 = model.numExpr();
				for (int j = 0; j < data.vertex_num; j++) {
					if (data.arcs[i][j] == 1) {
						expr7 = model.sum(expr7,x[i][j][k]);
					}
				}
				model.addLe(model.prod(data.a[i], expr7), w[i][k]);
				model.addLe(w[i][k], model.prod(data.b[i], expr7));
			}
		}
		//公式(8)
		for (int k = 0; k < data.veh_num; k++) {
			model.addLe(data.E, w[0][k]);
			model.addLe(data.E, w[data.vertex_num-1][k]);
			model.addLe(w[0][k], data.L);
			model.addLe(w[data.vertex_num-1][k], data.L);
		}
		//公式(9)
		for (int k = 0; k < data.veh_num; k++) {
			IloNumExpr expr8 = model.numExpr();
			for (int i = 1; i < data.vertex_num-1; i++) {
				IloNumExpr expr9 = model.numExpr();
				for (int j = 0; j < data.vertex_num; j++) {
					if (data.arcs[i][j] == 1) {
						expr9=model.sum(expr9,x[i][j][k]);
					}
				}
				expr8 = model.sum(expr8,model.prod(data.demands[i],expr9));
			}
			model.addLe(expr8, data.cap);
		}
	}
	//函数功能：解模型，并生成车辆路径和得到目标值
	//获取cplex解
	public void get_value() throws IloException {
		routes.clear();
		servetimes.clear();
		cost = 0;
//		//初始化车辆路径和花费时间链表，链表长度为车辆数k
		for (int k = 0; k < data.veh_num; k++) {
			ArrayList<Integer> r = new ArrayList<>();	
			ArrayList<Double> t = new ArrayList<>();	
			routes.add(r);							
			servetimes.add(t);							
		}
		for (int i = 0; i < data.vertex_num; i++) {
			for (int j = 0; j < data.vertex_num; j++) {
				for (int k = 0; k < data.veh_num; k++) {
					x_map[i][j][k] = 0.0;
				}
				if (data.arcs[i][j]>0.5) {
					for (int k = 0; k < data.veh_num; k++) {
						x_map[i][j][k]=model.getValue(x[i][j][k]);
					}
				}
			}
		}
		//模型可解，生成车辆路径
		for(int k = 0; k < data.veh_num; k++){
			boolean terminate = true;
			int i = 0;
			routes.get(k).add(0);		
			servetimes.get(k).add(0.0);
			while(terminate){
				for (int j = 0; j < data.vertex_num; j++) {
					if (doubleCompare(x_map[i][j][k], 0)==1) {
						routes.get(k).add(j);
						servetimes.get(k).add(model.getValue(w[j][k]));
						i = j;
						break;
					}
				}
				if (i == data.vertex_num-1) {
					terminate = false;
				}
			}
		}
		cost = model.getObjValue();
	}
	//branch and bound过程
	public void branch_and_bound(BaB_Vrptw lp) throws IloException {
		cur_best = 3000;//设置上届
		deep=0;
		record_arc = new int[3];
		node1 = new Node(data);
		best_note = null;
		queue = new PriorityQueue<Node>();
		//初始解（非法解）
		for (int i = 0; i < lp.routes.size(); i++) {
			ArrayList<Integer> r = lp.routes.get(i);
			System.out.println();
			for (int j = 0; j < r.size(); j++) {
				System.out.print(r.get(j)+" ");
			}
		}
		lp.copy_lp_to_node(lp, node1);
//		node1.node_cost = lp.cost;
//		node1.lp_x = lp.x_map.clone();
//		node1.node_routes =lp.routes;
//		node1.node_servetimes = lp.servetimes;
		node2 = node1.note_copy();
		deep=0;
		node1.d=deep;
		queue.add(node1);
		//branch and bound过程
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			//某支最优解大于当前最好可行解，删除
			if (doubleCompare(node.node_cost, cur_best)>0) {
				continue;
			}else {
				record_arc = lp.find_arc(node.lp_x);
				//某支的合法解,0,1组合的解,当前分支最好解
				if (record_arc[0]==-1) {
					//比当前最好解好，更新当前解
					if (doubleCompare(node.node_cost, cur_best)==-1) {
						lp.cur_best = node.node_cost;
						System.out.println(node.d+"  cur_best:"+cur_best);
						lp.best_note = node;
					}
					continue;
				}else {//可以分支
					node1 = lp.branch_left_arc(lp, node, record_arc);//左支
					node2 = lp.branch_right_arc(lp, node, record_arc);//右支
					if (node1!=null && doubleCompare(node1.node_cost, cur_best)<=0) {
						queue.add(node1);
					}
					if (node2!=null && doubleCompare(node2.node_cost, cur_best)<=0) {
						queue.add(node2);
					}
				}
			}
		}
	}
	//分支设置
	public void set_bound(Node node) throws IloException {
		for (int i = 0; i < data.vertex_num; i++) {
			for (int j = 0; j < data.vertex_num; j++) {
				if (data.arcs[i][j]>0.5) {
					if (node.node_x[i][j]==0) {
						for (int k = 0; k < data.veh_num; k++) {
							x[i][j][k].setLB(0.0);
							x[i][j][k].setUB(1.0);
						}
					}else if (node.node_x[i][j]==-1) {
						for (int k = 0; k < data.veh_num; k++) {
							x[i][j][k].setLB(0.0);
							x[i][j][k].setUB(0.0);
						}
					}else {
						for (int k = 0; k < data.veh_num; k++) {
							if (node.node_x_map[i][j][k]==1) {
								x[i][j][k].setLB(1.0);
								x[i][j][k].setUB(1.0);
							}else {
								x[i][j][k].setLB(0.0);
								x[i][j][k].setUB(0.0);
							}
						}
					}
				}
			}
		}
	}
	public void set_bound1(Node node) throws IloException {
		for (int i = 0; i < data.vertex_num; i++) {
			for (int j = 0; j < data.vertex_num; j++) {
				if (data.arcs[i][j]>0.5) {
					for (int k = 0; k < data.veh_num; k++) {
						if (node.node_x_map[i][j][k]==0) {
							x[i][j][k].setLB(0.0);
							x[i][j][k].setUB(1.0);
						}else if (node.node_x_map[i][j][k]==-1) {
							x[i][j][k].setLB(0.0);
							x[i][j][k].setUB(0.0);
						}else {
							x[i][j][k].setLB(1.0);
							x[i][j][k].setUB(1.0);
						}
					}
				}
			}
		}
	}
	//设置左支
	public Node branch_left_arc(BaB_Vrptw lp,Node father_node,int[] record) throws IloException {
		if (record[0] == -1) {
			return null;
		}
		Node new_node = new Node(data);
		new_node = father_node.note_copy();
		new_node.node_x[record[0]][record[1]] = -1;
		for (int k = 0; k < data.veh_num; k++) {
			new_node.node_x_map[record[0]][record[1]][k]=0;
		}
//		new_node.node_x_map[record[0]][record[1]][record[2]]=-1;
		//设置左支
		lp.set_bound(new_node);
		
		if (lp.model.solve()) {
			lp.get_value();
			deep++;
			new_node.d=deep;
			lp.copy_lp_to_node(lp, new_node);
			System.out.println(new_node.d+" "+lp.cost);
		}else {
			new_node.node_cost = data.big_num;
		}
		return new_node;
}
	//设置右支
	public Node branch_right_arc(BaB_Vrptw lp,Node father_node,int[] record) throws IloException {
		if (record[0] == -1) {
			return null;
		}
		Node new_node = new Node(data);
		new_node = father_node.note_copy();
		new_node.node_x[record[0]][record[1]] = 1;
//		new_node.node_x_map[record[0]][record[1]][record[2]]=1;
		for (int k = 0; k < data.veh_num; k++) {
			if (k==record[2]) {
				new_node.node_x_map[record[0]][record[1]][k]=1;
			}else {
				new_node.node_x_map[record[0]][record[1]][k]=0;
			}
		}
		//设置右支
		lp.set_bound(new_node);
		if (lp.model.solve()) {
			lp.get_value();
			deep++;
			new_node.d=deep;
			System.out.println(new_node.d+" right: "+lp.cost);
			lp.copy_lp_to_node(lp, new_node);
		}else {
			new_node.node_cost = data.big_num;
		}
		return new_node;
	}
	//找到需要分支的支点位置
	public int[] find_arc1(double[][][] x) {
		int record[] = new int[3];//记录分支顶点
		double cur_dif = 0;
		double min_dif = Double.MAX_VALUE;
		//找出最接近0.5的弧
		for (int i = 1; i <data.vertex_num-1; i++) {
			for (int j = 1; j < data.vertex_num-1; j++) {
				if (data.arcs[i][j]>0.5) {
					for (int k = 0; k <data.veh_num; k++) {
						//若该弧值为0或1，则继续
						if (is_one_zero(x[i][j][k])) {
							continue;
						}
						cur_dif = get_dif(x[i][j][k]);
						if (doubleCompare(cur_dif, min_dif)==-1) {
							record[0] = i;
							record[1] = j;
							record[2] = k;
							min_dif = cur_dif;
						}
					}
				}
			}
		}
		//depot
		if (doubleCompare(min_dif, Double.MAX_VALUE)==0) {
			for (int i = 1; i < data.vertex_num-1; i++) {
				if (data.arcs[0][i]>0.5) {
					for (int k = 0; k < data.veh_num; k++) {
						if (is_fractional(x[0][i][k])) {
							cur_dif = get_dif(x[0][i][k]);
							if (doubleCompare(cur_dif, min_dif)==-1) {
								record[0] = 0;
								record[1] = i;
								record[2] = k;
								min_dif = cur_dif;
							}
						}
					}
				}
				if (data.arcs[i][data.vertex_num-1]>0.5) {
					for (int k = 0; k < data.veh_num; k++) {
						if (is_fractional(x[i][data.vertex_num-1][k])) {
							cur_dif = get_dif(x[i][data.vertex_num-1][k]);
							if (doubleCompare(cur_dif, min_dif)==-1) {
								record[0] = i;
								record[1] = data.vertex_num-1;
								record[2] = k;
								min_dif = cur_dif;
							}
						}
					}
				}
			}
		}
		if (doubleCompare(min_dif, data.big_num)==1) {
			record[0] = -1;
			record[1] = -1;
			record[2] = -1;
		}
		return record;
	}
//	找到要分支的弧
	public int[] find_arc(double[][][] x) {
		int record[] = new int[3];//记录分支顶点
		for (int i = 0; i <data.vertex_num; i++) {
			for (int j = 0; j < data.vertex_num; j++) {
				if (data.arcs[i][j]>0.5) {
					for (int k = 0; k <data.veh_num; k++) {
						//若该弧值为0或1，则继续
						if (is_one_zero(x[i][j][k])) {
							continue;
						}
//						cur_dif = get_dif(x[i][j][k]);
						record[0] = i;
						record[1] = j;
						record[2] = k;
						return record;
					}
				}
			}
		}
		record[0] = -1;
		record[1] = -1;
		record[2] = -1;
		return record;
	}
	//比较两个double数值的大小
	public int doubleCompare(double a, double b){
		if(a - b > x_gap)
			return 1;
		if(b - a > x_gap)
			return -1;		
		return 0;
	} 
	//判断是否为0到1之间的小数
	public boolean is_fractional(double v){
		if( v > (int) v + x_gap && v < (int) v + 1 - x_gap)
			return true;
		else
			return false;
	}
	//判断是否为0或者1
	public boolean is_one_zero(double temp) {
		if (doubleCompare(temp, 0)==0 || doubleCompare(temp, 1)==0) {
			return true;
		}else {
			return false;
		}
	}
	//获取到0.5的距离
	public double get_dif(double temp) {
		double v = (int)temp+0.5;
		if (v>temp) {
			return v-temp;
		} else {
			return temp-v;
		}
	}
	public BaB_Vrptw init(BaB_Vrptw lp) throws IloException {
		lp.build_model();
		if (lp.model.solve()) {
			lp.get_value();
			int aa=0;
			for (int i = 0; i < lp.routes.size(); i++) {
				if (lp.routes.get(i).size()==2) {
					aa++;
				}
			}
			System.out.println(aa);
			if (aa==0) {
				data.veh_num -=1;
				lp.model.clearModel();
				lp = new BaB_Vrptw(data);
				return init(lp);
			}else {
				data.veh_num -=aa;
				lp.model.clearModel();
				lp = new BaB_Vrptw(data);
				return init(lp);
			}
		}else {
			data.veh_num +=1;
			System.out.println("vehicle number: "+data.veh_num);
			lp.model.clearModel();
			lp = new BaB_Vrptw(data);
			lp.build_model();
			if (lp.model.solve()) {
				lp.get_value();
				return lp;
			}else {
				System.out.println("error init");
				return null;
			}
		}
	}
	public static void main(String[] args) throws Exception {
		Data data = new Data();
		int vetexnum = 31;//所有点个数，包括0，n+1两个配送中心点
		//读入不同的文件前要手动修改vetexnum参数，参数值等于所有点个数,包括配送中心
		String path = "data/3022.txt";//算例地址
		data.Read_data(path,data,vetexnum);
		System.out.println("input succesfully");
		System.out.println("cplex procedure###########################");
		BaB_Vrptw lp = new BaB_Vrptw(data);
		double cplex_time1 = System.nanoTime();
		//删除未用的车辆，缩小解空间
		lp=lp.init(lp);
		System.out.println(":   "+lp.data.veh_num);
		lp.branch_and_bound(lp);
		Check check = new Check(lp);
		check.fesible();
		double cplex_time2 = System.nanoTime();
		double cplex_time = (cplex_time2 - cplex_time1) / 1e9;//求解时间，单位s
		System.out.println("cplex_time " + cplex_time + " bestcost " + lp.cur_best);
		for (int i = 0; i < lp.best_note.node_routes.size(); i++) {
			ArrayList<Integer> r = lp.best_note.node_routes.get(i);
			System.out.println();
			for (int j = 0; j < r.size(); j++) {
				System.out.print(r.get(j)+" ");
			}
		}
	}
}
