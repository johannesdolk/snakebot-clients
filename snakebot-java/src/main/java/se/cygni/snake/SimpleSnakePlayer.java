package se.cygni.snake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.cygni.snake.api.event.GameEndedEvent;
import se.cygni.snake.api.event.GameStartingEvent;
import se.cygni.snake.api.event.MapUpdateEvent;
import se.cygni.snake.api.event.SnakeDeadEvent;
import se.cygni.snake.api.exception.InvalidPlayerName;
import se.cygni.snake.api.model.GameMode;
import se.cygni.snake.api.model.GameSettings;
import se.cygni.snake.api.model.SnakeDirection;
import se.cygni.snake.api.response.PlayerRegistered;
import se.cygni.snake.client.AnsiPrinter;
import se.cygni.snake.client.BaseSnakeClient;
import se.cygni.snake.client.MapCoordinate;
import se.cygni.snake.client.MapUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class SimpleSnakePlayer extends BaseSnakeClient {

    private static Logger log = LoggerFactory
            .getLogger(SimpleSnakePlayer.class);

    private AnsiPrinter ansiPrinter;

    public static void main(String[] args) {
        Runnable task = () -> {
            SimpleSnakePlayer sp = new SimpleSnakePlayer();
            sp.connect();

            // Keep this process alive as long as the
            // Snake is connected and playing.
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (sp.isPlaying());

            log.info("Shutting down");
        };


        Thread thread = new Thread(task);
        thread.start();
    }

    private SimpleSnakePlayer() {
        ansiPrinter = new AnsiPrinter(true);
    }

    @Override
    public void onMapUpdate(MapUpdateEvent mapUpdateEvent) {
        ansiPrinter.printMap(mapUpdateEvent);

        // MapUtil contains lot's of useful methods for querying the map!
        MapUtil mapUtil = new MapUtil(mapUpdateEvent.getMap(), getPlayerId());
        List<SnakeDirection> directions;

        Optional<MapCoordinate> mapCoordinate = shouldGoForFood(mapUtil);
        Optional<SnakeDirection> chosenDirection;
        if (mapCoordinate.isPresent()) {
            directions = moveAgainstFood(mapCoordinate.get(), mapUtil);
            chosenDirection = directions.stream().filter(mapUtil::canIMoveInDirection).findAny();
            if (chosenDirection.isPresent()) {
                registerMove(mapUpdateEvent.getGameTick(), chosenDirection.get());
            }
        } else {
            directions = new ArrayList<>();
            // Let's see in which directions I can move
            for (SnakeDirection direction : SnakeDirection.values()) {
                if (mapUtil.canIMoveInDirection(direction)) {
                    directions.add(direction);
                }
            }

            Random r = new Random();
            chosenDirection = Optional.of(SnakeDirection.DOWN);

            // Choose a random direction
            if (!directions.isEmpty()) {
                chosenDirection = Optional.of(directions.get(r.nextInt(directions.size())));
            }
            // Register action here!

            registerMove(mapUpdateEvent.getGameTick(), chosenDirection.get());
        }

    }

    private List<SnakeDirection> moveAgainstFood(MapCoordinate mapCoordinate, MapUtil mapUtil) {
        List<SnakeDirection> directions = new ArrayList<>();
        if (mapCoordinate.x > mapUtil.getMyPosition().x) {
            directions.add(SnakeDirection.RIGHT);
        } else if (mapCoordinate.x < mapUtil.getMyPosition().x) {
            directions.add(SnakeDirection.LEFT);
        }
        if (mapCoordinate.y > mapUtil.getMyPosition().y) {
            directions.add(SnakeDirection.DOWN);
        } else if (mapCoordinate.y < mapUtil.getMyPosition().y) {
            directions.add(SnakeDirection.UP);
        }
        return directions;
    }

    private Optional<MapCoordinate> shouldGoForFood(MapUtil mapUtil) {
        MapCoordinate myPosition = mapUtil.getMyPosition();
        return Stream.of(mapUtil.listCoordinatesContainingFood()).reduce((mapCoordinate, mapCoordinate2) -> (mapCoordinate.getManhattanDistanceTo(myPosition) < mapCoordinate2.getManhattanDistanceTo(myPosition) ? mapCoordinate : mapCoordinate2));
    }


    @Override
    public void onInvalidPlayerName(InvalidPlayerName invalidPlayerName) {

    }

    @Override
    public void onSnakeDead(SnakeDeadEvent snakeDeadEvent) {
        log.info("A snake {} died by {}",
                snakeDeadEvent.getPlayerId(),
                snakeDeadEvent.getDeathReason());
    }

    @Override
    public void onGameEnded(GameEndedEvent gameEndedEvent) {
        log.debug("GameEndedEvent: " + gameEndedEvent);
    }

    @Override
    public void onGameStarting(GameStartingEvent gameStartingEvent) {
        log.debug("GameStartingEvent: " + gameStartingEvent);
    }

    @Override
    public void onPlayerRegistered(PlayerRegistered playerRegistered) {
        log.info("PlayerRegistered: " + playerRegistered);

        // Disable this if you want to start the game manually from
        // the web GUI
//        startGame();
    }

    @Override
    public void onSessionClosed() {
        log.info("Session closed");
    }

    @Override
    public void onConnected() {
        log.info("Connected, registering for training...");
        GameSettings gameSettings = new GameSettings.GameSettingsBuilder()
                .withWidth(25)
                .withHeight(25)
                .withMaxNoofPlayers(5)
                .build();

        registerForGame(gameSettings);
    }

    @Override
    public String getName() {
        return "#emil";
    }

    @Override
    public String getServerHost() {
        return "localhost";
    }

    @Override
    public int getServerPort() {
        return 8080;
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.training;
    }
}
