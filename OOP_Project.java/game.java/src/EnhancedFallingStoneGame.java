import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import javax.swing.Timer;

abstract class GameObject {
    protected int x, y, speed;

    public GameObject(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSpeed() {
        return speed;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}


class Stone extends GameObject {
    public Stone(int x, int y, int speed) {
        super(x, y, speed);
    }
}

public class EnhancedFallingStoneGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int playerX = 250, playerY = 550;
    private final int playerWidth = 40, playerHeight = 50;
    private int score = 0;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private boolean isPaused = false;

    private final List<Stone> stones = new ArrayList<>();
    private final Random random = new Random();
    private Image playerImage, stoneImage;
    private Clip hitSound, backgroundMusic;
    private List<Integer> highScores = new ArrayList<>();

    public EnhancedFallingStoneGame() {
        JFrame frame = new JFrame("Enhanced Falling Stone Game");
        frame.setSize(600, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        this.setBackground(Color.CYAN);
        this.addKeyListener(this);
        this.setFocusable(true);
        this.requestFocusInWindow();
        frame.setVisible(true);

        createTimer();
        loadHighScores();
        loadImages();
        loadSounds();
    }

    private void createTimer() {
        timer = new Timer(10, this);
    }

    private void loadImages() {
        try {
            playerImage = new ImageIcon(getClass().getResource("/player.png")).getImage();
            stoneImage = new ImageIcon(getClass().getResource("/stone.png")).getImage();
        } catch (Exception e) {
            System.out.println("Image loading error: " + e.getMessage());
        }
    }

    private void loadSounds() {
        try {
            URL hitUrl = getClass().getResource("/hit.wav");
            if (hitUrl != null) {
                AudioInputStream hitAudio = AudioSystem.getAudioInputStream(hitUrl);
                hitSound = AudioSystem.getClip();
                hitSound.open(hitAudio);
            }
        } catch (Exception e) {
            System.out.println("Hit sound error: " + e.getMessage());
        }
        loadBackgroundMusic();
    }

    private void loadBackgroundMusic() {
        try {
            if (backgroundMusic != null)
                backgroundMusic.close();
            URL soundURL = getClass().getResource("/background.wav");
            if (soundURL != null) {
                AudioInputStream backgroundAudio = AudioSystem.getAudioInputStream(soundURL);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(backgroundAudio);
            }
        } catch (Exception e) {
            System.out.println("Background music error: " + e.getMessage());
        }
    }

    private void loadHighScores() {
        highScores.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader("highscores.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    highScores.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            highScores.sort(Collections.reverseOrder());
            if (highScores.size() > 5) {
                highScores = new ArrayList<>(highScores.subList(0, 5));
            }
        } catch (IOException ignored) {
        }
    }

    private void saveHighScores() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("highscores.txt"))) {
            for (int score : highScores) {
                writer.write(score + "\n");
            }
        } catch (IOException e) {
            System.out.println("Error saving high scores.");
        }
    }

    private void resetHighScores() {
        highScores.clear();
        saveHighScores();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.RED);
            g.drawString("Game Over!", 180, 300);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Score: " + score, 270, 340);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("High Scores:", 220, 380);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            int y = 420;
            for (int i = 0; i < highScores.size(); i++) {
                g.drawString((i + 1) + ". " + highScores.get(i), 250, y);
                y += 30;
            }
            g.setColor(Color.DARK_GRAY);
            g.drawString("Press R to reset high scores", 180, 600);
        } else if (!gameStarted) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.BLUE);
            g.drawString("Press ENTER to Start", 120, 300);
        } else {
            if (isPaused) {
                g.setFont(new Font("Arial", Font.BOLD, 40));
                g.setColor(Color.ORANGE);
                g.drawString("Paused", 230, 300);
                return;
            }

            if (playerImage != null) {
                g.drawImage(playerImage, playerX, playerY, playerWidth, playerHeight, this);
            } else {
                g.setColor(Color.BLUE);
                g.fillRect(playerX, playerY, playerWidth, playerHeight);
            }

            for (Stone stone : stones) {
                if (stoneImage != null) {
                    g.drawImage(stoneImage, stone.getX(), stone.getY(), 30, 30, this);
                } else {
                    g.setColor(Color.GRAY);
                    g.fillOval(stone.getX(), stone.getY(), 30, 30);
                }
            }

            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.BLACK);
            g.drawString("Score: " + score, 20, 30);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameStarted || gameOver || isPaused)
            return;

        if (random.nextInt(10) == 0) {
            int speed = 3 + score / 10;
            stones.add(new Stone(random.nextInt(570), 0, speed));
        }

        Iterator<Stone> it = stones.iterator();
        while (it.hasNext()) {
            Stone stone = it.next();
            stone.setY(stone.getY() + stone.getSpeed());

            boolean collision = (stone.getY() + 20 >= playerY) &&
                    (stone.getY() <= playerY + playerHeight) &&
                    (stone.getX() + 20 >= playerX) &&
                    (stone.getX() <= playerX + playerWidth);

            if (collision) {
                gameOver = true;
                timer.stop();
                if (hitSound != null) {
                    hitSound.setFramePosition(0);
                    hitSound.start();
                }
                if (backgroundMusic != null) {
                    backgroundMusic.stop();
                    backgroundMusic.close();
                }
                highScores.add(score);
                highScores.sort(Collections.reverseOrder());
                if (highScores.size() > 5) {
                    highScores = new ArrayList<>(highScores.subList(0, 5));
                }
                saveHighScores();
            }

            if (stone.getY() > 650) {
                it.remove();
                score++;
            }
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (gameOver) {
            if (code == KeyEvent.VK_ENTER) {
                restartGame();
            } else if (code == KeyEvent.VK_R) {
                resetHighScores();
            }
            return;
        }

        if (!gameStarted && code == KeyEvent.VK_ENTER) {
            gameStarted = true;
            loadBackgroundMusic();
            if (backgroundMusic != null) {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
            timer.start();
        }

        if (code == KeyEvent.VK_P && gameStarted && !gameOver) {
            isPaused = !isPaused;
            if (isPaused) {
                timer.stop();
                if (backgroundMusic != null)
                    backgroundMusic.stop();
            } else {
                timer.start();
                if (backgroundMusic != null)
                    backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
            repaint();
            return;
        }

        if (!isPaused) {
            if (code == KeyEvent.VK_LEFT && playerX > 0)
                playerX -= 20;
            if (code == KeyEvent.VK_RIGHT && playerX < 550)
                playerX += 20;
            if (code == KeyEvent.VK_UP && playerY > 0)
                playerY -= 20;
            if (code == KeyEvent.VK_DOWN && playerY < (600 - playerHeight))
                playerY += 20;
            repaint();
        }
    }

    private void restartGame() {
        gameOver = false;
        gameStarted = true;
        score = 0;
        stones.clear();
        playerX = 250;
        playerY = 550;
        isPaused = false;

        loadBackgroundMusic();
        if (backgroundMusic != null) {
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }

        createTimer();
        timer.start();
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EnhancedFallingStoneGame::new);
    }
}
