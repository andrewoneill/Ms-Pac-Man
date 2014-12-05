package pacman.entries.ghosts;

import java.io.File;
import java.util.EnumMap;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.awt.Point;;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */
public class MyGhosts extends Controller<EnumMap<GHOST,MOVE>>
{
	public static final int CROWDED_DISTANCE = 40;
	public static final int PACMAN_DISTANCE = 20;
	public static final int PILL_PROXIMITY = 20;
	
	private final static float CONSISTENCY=0.8f;	//carry out intended move with this probability
	private Random rnd=new Random();
	
	private EnumMap<GHOST, MOVE> myMoves=new EnumMap<GHOST, MOVE>(GHOST.class);
	private final EnumMap<GHOST, Integer> cornerAllocation = new EnumMap<GHOST, Integer>(GHOST.class);
	
	private MOVE[] moves = MOVE.values();
	private String myState = "chase";
	private String myEvent= "none";
	private String myAction = "chase";
	
	public MyGhosts(){
		cornerAllocation.put(GHOST.BLINKY,0);
    	cornerAllocation.put(GHOST.INKY,1);
    	cornerAllocation.put(GHOST.PINKY,2);
    	cornerAllocation.put(GHOST.SUE,3);
	}
	public void FSM() {
		try {
			File fXmlFile = new File(
					"XML's/fsm.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			(doc).getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("state");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				Element eElement = (Element) nNode;
				if (eElement.getElementsByTagName("currentstate").item(0).getTextContent().equals(myState)
						&& eElement.getElementsByTagName("event").item(0).getTextContent().equals(myEvent)) {
					myAction = eElement.getElementsByTagName("action").item(0).getTextContent();
					myState = eElement.getElementsByTagName("newstate").item(0).getTextContent();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue)
	{
		myMoves.clear();
		
		FSM();
		for(GHOST ghost : GHOST.values())
			if(game.doesGhostRequireAction(ghost)){
				int currentIndex=game.getGhostCurrentNodeIndex(ghost);
				int pacmanX = game.getNodeXCood(game.getPacmanCurrentNodeIndex());
				int pacmanY = game.getNodeYCood(game.getPacmanCurrentNodeIndex());
				int ghostX = game.getNodeXCood(game.getGhostCurrentNodeIndex(ghost));
				int ghostY = game.getNodeXCood(game.getGhostCurrentNodeIndex(ghost));
				Point pacmanPoint = new Point(pacmanX,pacmanY);
				Point ghostPoint = new Point(ghostX,ghostY);
				int radius = 50;
				
				if(game.getGhostEdibleTime(ghost)<1)
					myEvent ="notedible";
				if(game.getGhostEdibleTime(ghost)>=1)
					myEvent = "edible";
				if(closeToPower(game,ghost) && pacmanPoint.distance(ghostPoint) < radius)
					myEvent="flee";
				if(isCrowded(game))
					myEvent = "crowded";
				if(myState.equals("flee")|| (myEvent.equals("crowded") && !closeToMsPacMan(game, currentIndex)))
					getRetreatActions(game,ghost);
				if(myState.equals("chase")){
					if(rnd.nextFloat()<CONSISTENCY)
						myMoves.put(ghost,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
								game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
					else
						myMoves.put(ghost,moves[rnd.nextInt(moves.length)]);
				}
				if(ghost.equals(GHOST.BLINKY) && myEvent.equals("notedible")){
					pacmanX = game.getNodeXCood(game.getPacmanCurrentNodeIndex());
					pacmanY = game.getNodeYCood(game.getPacmanCurrentNodeIndex());
					ghostX = game.getNodeXCood(game.getGhostCurrentNodeIndex(ghost));
					ghostY = game.getNodeXCood(game.getGhostCurrentNodeIndex(ghost));
					pacmanPoint = new Point(pacmanX,pacmanY);
					ghostPoint = new Point(ghostX,ghostY);
					radius = 60;
					if( pacmanPoint.distance(ghostPoint) < radius){
						myMoves.put(ghost,game.getNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
								game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.MANHATTAN));
					}
				}
				if(ghost.equals(GHOST.INKY) && myEvent.equals("notedible")){
					int attackNode = game.getNeighbour(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());
					try{
						int neighboutNode = 0;
						for(int i = 0; i < 16; i++)
						{
							neighboutNode = game.getNeighbour(attackNode, game.getPacmanLastMoveMade());
							if( neighboutNode != -1){
								attackNode = neighboutNode;
							}
						}
					}catch(Exception e){		
					}
				
					if( attackNode == -1){
						myMoves.put(ghost,game.getNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
								game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.MANHATTAN));
					}
					else{
						MOVE attackMove = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost), attackNode, game.getGhostLastMoveMade(ghost), DM.MANHATTAN);
						myMoves.put(ghost,attackMove);
					}
				}
			}
		return myMoves;
	}
	private boolean closeToMsPacMan(Game game,int location)
	 {
	    	if(game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(),location)<PACMAN_DISTANCE)
	    		return true;

	    	return false;
	 }
	private boolean closeToPower(Game game, GHOST ghost) {
		//get ghost index
		int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
		//get pacman index
		int pacmanIndex = game.getPacmanCurrentNodeIndex();
		// create power pill indices
		int[] powerPillIndices = game.getActivePowerPillsIndices();
		if (powerPillIndices.length > 0) {
			// get nearest pill
			int index = getNearestPill(game, ghostIndex);

			int closest = (game.getShortestPathDistance(
					powerPillIndices[index], ghostIndex));
			if (game.getShortestPathDistance(powerPillIndices[index],
					pacmanIndex) < closest
					&& game.getShortestPathDistance(powerPillIndices[index],
							pacmanIndex) < PILL_PROXIMITY)
				return true;
		}
		return false;
	}
	private boolean isCrowded(Game game)
    {
    	GHOST[] ghosts=GHOST.values();
        float distance=0;
        
        for (int i=0;i<ghosts.length-1;i++)
            for(int j=i+1;j<ghosts.length;j++)
                distance+=game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghosts[i]),game.getGhostCurrentNodeIndex(ghosts[j]));
        
        return (distance/6)<CROWDED_DISTANCE ? true : false;
    }
	private int getNearestPill(Game game,int index){
		int node = 0;
		int[] powerPillIndices = game.getActivePowerPillsIndices();
		//if there are sill pills left
		if(powerPillIndices.length > 0) {
			//init closest
			int closest = game.getShortestPathDistance(powerPillIndices[0],
					index);
			
			// for all active power pills
			for (int i = 0; i < powerPillIndices.length; i++) {
				//if there is a closer power pill
				if (closest >= game.getShortestPathDistance(powerPillIndices[i],
						index)) {
					// store index of the closest pill
					closest = game.getShortestPathDistance(powerPillIndices[i],
							index);
					node = i;
				}
			}
		}
		
		return node;
	}
	private MOVE getRetreatActions(Game game,GHOST ghost)
	    {
	    	int currentIndex=game.getGhostCurrentNodeIndex(ghost);
	    	int pacManIndex=game.getPacmanCurrentNodeIndex();
	    	
	        if(game.getGhostEdibleTime(ghost)==0 && game.getShortestPathDistance(currentIndex,pacManIndex)<PACMAN_DISTANCE)
	            return game.getApproximateNextMoveTowardsTarget(currentIndex,pacManIndex,game.getGhostLastMoveMade(ghost),DM.PATH);
	        else
	            return game.getApproximateNextMoveTowardsTarget(currentIndex,game.getPowerPillIndices()[cornerAllocation.get(ghost)],game.getGhostLastMoveMade(ghost),DM.PATH);
	    }
}