package SP18_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import javax.tools.DocumentationTool.Location;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다. 
 * <br><br>
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다.<br>
 * - program code를 메모리에 적재시키기<br>
 * - 주어진 공간만큼 메모리에 빈 공간 할당하기<br>
 * - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
	ResourceManager rMgr;
	SymbolTable symtab;
	ArrayList<String> mcode = new ArrayList<String>();
	ArrayList<Integer> mcodeAddress = new ArrayList<Integer>();
	int currentSection;
	int pacount=0;
	public SicLoader(ResourceManager resourceManager) {
		// 필요하다면 초기화
		setResourceManager(resourceManager);
		symtab = new SymbolTable();
		currentSection =0;
	}
	/**
	 * Loader와 프로그램을 적재할 메모리를 연결시킨다.
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr=resourceManager;
	}
	/**
	 * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록 한다.
	 * load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
	 * @param objectCode 읽어들인 파일
	 */
	public void load(File objectCode){ 
			
		    String line;
		    String[] name;
		    String[] define;
		    int mem =0;
		    int tmem =0;
		    int secaddr = 0;  //이전 시작 주소  변수
		    BufferedReader br = null;
		    // pass 1
		    try{
		      br = new BufferedReader(new InputStreamReader(new FileInputStream(objectCode)));
		      
		      while((line = br.readLine()) != null){
		    	  if(line.length() != 0) {
		    		/* input.obj에서 읽으면서 앞글자 H, T, M로 구분해서 로딩 처리 */
				    switch(line.charAt(0)) {
				    case 'H':
				    	name = line.substring(1).split("\t");
				    	
				    	rMgr.setProgname(name[0], currentSection);
				    	rMgr.setProgLength(name[1].substring(6), currentSection);
				    	if(currentSection!=0) {
				    		rMgr.setStartADDR(Integer.parseInt(name[1].substring(0, 6),16)+secaddr);
				    		symtab.putSymbol(name[0],Integer.parseInt(name[1].substring(0, 6),16)+secaddr);
				    	}
				    	else {
				    		rMgr.setStartADDR(Integer.parseInt(name[1].substring(0,6)));
				    		symtab.putSymbol(name[0],Integer.parseInt(name[1].substring(0,6)));
				    	}
				    	break;
				    case 'D':
				    	define = line.substring(1).split(" ");
				    	for(int i=0;i<define.length;) {
				    		symtab.putSymbol(define[i], Integer.parseInt(define[i+1],16));
				    		i+=2;
				    	}
				    	break;		    
				    case 'T':
				    	mem = Integer.parseInt(line.substring(1, 7),16) + rMgr.startAddr.get(currentSection);
				    	int ttmem=0, tmem1=0, tmem2=0;
				    	for(int i=0;i<line.substring(9).length();) {				    		
				    		tmem1 = Integer.parseInt(String.valueOf(line.charAt(9+i)),16);
				    		tmem2 = Integer.parseInt(String.valueOf(line.charAt(10+i)),16);
				    		
				    		if(tmem1>='0'&&tmem1<='9') {
				    			tmem1 -= '0';
				    		}
				    		else if(tmem2>='0'&&tmem2<='9') {
				    			tmem2 -= '0';
				    		}
				    		else if(tmem1>='A'&&tmem1<='F') {
				    			tmem1 -= 'A';
				    		}
				    		else if(tmem2>='A'&&tmem2<='F') {
				    			tmem2 -= 'A';
				    		}
				    		tmem1 = tmem1<<4;
				    		char[] abc = rMgr.intToChar(tmem1+tmem2, 1);
				    		rMgr.setMemory(mem, abc);
				    		
				    		i+=2;
				    		mem++;
	    						    		
				    	}
				    	break;
				    case 'M':
				    	mcode.add(line);
				    	mcodeAddress.add(secaddr);
			
				    	break;
				    case 'E':
				    	secaddr += Integer.parseInt(rMgr.progLength.get(currentSection),16);
				    	currentSection++;
				    	
				    	break;	
				    }
		    	  }
		    	   		  
		      }
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
		    //pass 2
		    /* Modification 처리 */
		    int zerocount=0; // 채워야할 0 개수 세서 개수에 따라 처리해줌
			int nowAddress;
		    for(int i=0;i<mcode.size();i++) {
		    	nowAddress = mcodeAddress.get(i);
		    	pacount = Integer.parseInt((mcode.get(i).substring(5,7)),16);
		    	pacount += nowAddress;
		    	int symAddres = symtab.search(mcode.get(i).substring(10));
		    	zerocount = (mcode.get(i).charAt(8));
		    
		    	if(zerocount == '5')
		    	{
		    		if(mcode.get(i).charAt(9) == '+')
		    			rMgr.setMemory((pacount + 1 ) ,rMgr.intToChar( (symAddres) , 2));
		    		if(mcode.get(i).charAt(9) == '-')
		    			rMgr.setMemory((pacount + 1 ) ,rMgr.intToChar( (symAddres) , 2));
		    	}
		    	if(zerocount == '6')
		    	{
		    		if(mcode.get(i).charAt(9) == '+')
		    			rMgr.setMemory((pacount) ,rMgr.intToChar( (symAddres) , 3));
		    		if(mcode.get(i).charAt(9) == '-')
		    			rMgr.setMemory((pacount) ,rMgr.intToChar( (symAddres) , 3));
		    	}
		    		    
		    	}

	};	

}
