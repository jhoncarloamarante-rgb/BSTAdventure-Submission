import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * BST Adventure - a graphical game for learning Binary Search Trees.
 * Compile with: javac -d bin src\Main.java
 * Run with: java -cp bin Main
 */
public class Main {
    private static Random random = new Random();
    private static final PracticeSearchRewards PRACTICE_SEARCH_REWARDS = new PracticeSearchRewards();
    private static int totalScore = 0;

    enum Difficulty {
        EASY(1, 5, 3, "Easy"),
        MEDIUM(2, 8, 2, "Medium"),
        HARD(3, 12, 1, "Hard");

        final int multiplier;
        final int extraNodes;
        final int mistakesAllowed;
        final String label;

        Difficulty(int multiplier, int extraNodes, int mistakesAllowed, String label) {
            this.multiplier = multiplier;
            this.extraNodes = extraNodes;
            this.mistakesAllowed = mistakesAllowed;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum CampaignMode {
        PRACTICE(true, "Practice (show sorted values)"),
        CHALLENGE(false, "Challenge (hide sorted values)");

        final boolean showSortedValues;
        final String label;

        CampaignMode(boolean showSortedValues, String label) {
            this.showSortedValues = showSortedValues;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum TraversalOrder {
        PREORDER("Pre-order", "Root, Left, Right"),
        INORDER("In-order", "Left, Root, Right"),
        POSTORDER("Post-order", "Left, Right, Root");

        final String label;
        final String rule;

        TraversalOrder(String label, String rule) {
            this.label = label;
            this.rule = rule;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum ScreenScene {
        MENU, CAMPAIGN, PRACTICE, TRAVERSAL, MINI_GAME
    }

    static class Node {
        int value;
        Node left;
        Node right;

        Node(int value) {
            this.value = value;
        }
    }

    static class NodeStack {
        private Node[] items = new Node[8];
        private int size;

        void push(Node node) {
            if (node == null) {
                throw new IllegalArgumentException("Stack cannot store a null node.");
            }
            if (size == items.length) {
                Node[] expanded = new Node[items.length * 2];
                System.arraycopy(items, 0, expanded, 0, items.length);
                items = expanded;
            }
            items[size] = node;
            size++;
        }

        Node pop() {
            if (isEmpty()) {
                throw new IllegalStateException("Cannot pop from an empty stack.");
            }
            size--;
            Node node = items[size];
            items[size] = null;
            return node;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }

    private static final Color BACKGROUND = new Color(243, 240, 223);
    private static final Color PANEL = new Color(255, 252, 241);
    private static final Color ACCENT = new Color(45, 106, 79);
    private static final Color TEXT = new Color(37, 53, 43);
    private static final Color MUTED = new Color(96, 111, 94);
    private static final Color BORDER = new Color(216, 201, 154);
    private static final Color BUTTON_COLOR = new Color(53, 107, 75);
    private static final Color BUTTON_HOVER = new Color(36, 85, 57);
    private static final Color SUCCESS = new Color(35, 112, 78);
    private static final Color WARNING = new Color(151, 101, 28);
    private static final Color FEEDBACK_ERROR = new Color(166, 57, 48);
    private static final Font BODY_FONT = new Font("Dialog", Font.PLAIN, 15);
    private static final Font HEADING_FONT = new Font("Serif", Font.BOLD, 27);
    private static final Path HIGH_SCORE_PATH = Paths.get(System.getProperty("user.dir"), "bst_highscores.txt");
    private static boolean largeTreeVisuals;
    private static boolean reducedMotion;
    private static boolean soundEnabled = true;

    private static class GameWindow extends JFrame {
        private static final long serialVersionUID = 1L;
        private final java.awt.CardLayout cards = new java.awt.CardLayout();
        private final JPanel screens = new JPanel(cards);
        private final JComboBox<Difficulty> difficultyChoice = new JComboBox<>(Difficulty.values());
        private final JComboBox<CampaignMode> modeChoice = new JComboBox<>(CampaignMode.values());
        private final JCheckBox largeVisualsChoice = new JCheckBox("Larger tree nodes and labels");
        private final JCheckBox reducedMotionChoice = new JCheckBox("Reduce animations");
        private final JCheckBox soundEffectsChoice = new JCheckBox("Enable sound effects");
        private final ScoreBadge scoreBadge = new ScoreBadge();
        private final StatusBadgeLabel campaignTitle = new StatusBadgeLabel();
        private final StatusBadgeLabel targetLabel = new StatusBadgeLabel();
        private final StatusBadgeLabel feedbackLabel = new StatusBadgeLabel("Choose a direction to follow the target.");
        private final StatusBadgeLabel mistakesLabel = new StatusBadgeLabel();
        private final StatusBadgeLabel streakLabel = new StatusBadgeLabel("Streak: 0 · Best: 0");
        private final StatusBadgeLabel practiceMessage = new StatusBadgeLabel("Insert values or search the tree.");
        private final JLabel practiceValues = new JLabel();
        private final StatusBadgeLabel traceStatus = new StatusBadgeLabel("Choose an order, start, then enter values in sequence.");
        private final StatusBadgeLabel traversalRule = new StatusBadgeLabel("In-order: Left, Root, Right");
        private final JLabel startOrderStatus = new JLabel("Choose a traversal order to begin.");
        private final StatusBadgeLabel traversalLevelLabel = new StatusBadgeLabel("Level 1");
        private final LifeBadgeLabel traversalHearts = new LifeBadgeLabel();
        private final ClockBadgeLabel traversalTimerLabel = new ClockBadgeLabel(10);
        private final JLabel arrangeValuesLabel = new JLabel();
        private final StatusBadgeLabel arrangeStatusLabel = new StatusBadgeLabel("Start a round to arrange the values.");
        private final LifeBadgeLabel arrangeHeartsLabel = new LifeBadgeLabel();
        private final StatusBadgeLabel placementQuestionLabel = new StatusBadgeLabel();
        private final StatusBadgeLabel placementStatusLabel = new StatusBadgeLabel("Choose whether the value belongs left or right.");
        private final LifeBadgeLabel placementHeartsLabel = new LifeBadgeLabel();
        private final StatusBadgeLabel drawTraceStatus = new StatusBadgeLabel("Click Start Trace to begin.");
        private final StatusBadgeLabel drawTraceOutput = new StatusBadgeLabel("Path: (none)");
        private final ClockBadgeLabel drawTraceTimerLabel = new ClockBadgeLabel(15);
        private final StatusBadgeLabel drawTraceRuleLabel = new StatusBadgeLabel("In-order: Left subtree, Root, Right subtree");
        private final JProgressBar traversalProgress = new JProgressBar();
        private final JProgressBar arrangeProgress = new JProgressBar();
        private final JProgressBar placementProgress = new JProgressBar();
        private final JProgressBar drawTraceProgress = new JProgressBar();
        private final CampaignTrail campaignTrail = new CampaignTrail();
        private final JTextArea traceOutput = new JTextArea("Your order: (none)", 2, 52);
        private final JTextField practiceInput = new JTextField(10);
        private final JTextField traversalValueInput = new JTextField(8);
        private final JTextField arrangeValueInput = new JTextField(8);
        private final JRadioButton arrangeInorderChoice = new JRadioButton("IN ORDER");
        private final JRadioButton arrangePreorderChoice = new JRadioButton("PRE ORDER");
        private final JRadioButton arrangePostorderChoice = new JRadioButton("POST ORDER");
        private final JRadioButton traceInorderChoice = new JRadioButton("IN ORDER");
        private final JRadioButton tracePreorderChoice = new JRadioButton("PRE ORDER");
        private final JRadioButton tracePostorderChoice = new JRadioButton("POST ORDER");
        private final JButton inorderChoice = button("In-order");
        private final JButton preorderChoice = button("Pre-order");
        private final JButton postorderChoice = button("Post-order");
        private final TreeView campaignTree = new TreeView();
        private final TreeView practiceTree = new TreeView();
        private final TreeView traversalTree = new TreeView();
        private final JButton leftButton = new JButton("L  Go Left");
        private final JButton rightButton = new JButton("Go Right  R");
        private final JButton advanceButton = new JButton("Continue");
        private final JButton placementLeftButton = button("Put it LEFT");
        private final JButton placementRightButton = button("Put it RIGHT");
        private final JButton placementNextLevelButton = button("Next Level");
        private final TreeView arrangeTree = new TreeView();
        private final TreeView placementTree = new TreeView();
        private final TreeView drawTraceTree = new TreeView();

        private Difficulty difficulty = Difficulty.EASY;
        private CampaignMode campaignMode = CampaignMode.PRACTICE;
        private Node campaignRoot;
        private Node currentNode;
        private Node practiceRoot;
        private Node traversalRoot;
        private Node arrangeRoot;
        private Node placementRoot;
        private Node drawTraceRoot;
        private Node placementCurrentNode;
        private List<Integer> arrangeSortedValues = Collections.emptyList();
        private List<Integer> arrangeDisplayedValues = Collections.emptyList();
        private List<Node> drawTraceNodes = Collections.emptyList();
        private final List<Node> drawnTraceNodes = new ArrayList<>();
        private int arrangeIndex;
        private int arrangeHearts = 3;
        private int placementValue;
        private int placementParentValue;
        private int placementRound;
        private int placementLevel = 1;
        private int placementHearts = 3;
        private boolean placementAnswerPending;
        private boolean drawTraceActive;
        private int drawTraceSecondsRemaining = 15;
        private final Timer drawTraceTimer = new Timer(1000, event -> tickDrawTraceTimer());
        private final Timer placementAdvanceTimer = new Timer(800, event -> beginPlacementQuestion());
        private List<Node> traceNodes = Collections.emptyList();
        private final List<Integer> tracedValues = new ArrayList<>();
        private final Set<Node> orderedNodes = new HashSet<>();
        private int campaignLevel;
        private int campaignMistakes;
        private int completedLevels;
        private int campaignBonus;
        private int target;
        private int currentStreak;
        private int bestStreak;
        private boolean campaignEnded;
        private boolean orderingActive;
        private int traversalHeartsRemaining = 3;
        private int traversalSecondsRemaining = 10;
        private int traversalLevel = 1;
        private final Timer traversalTimer = new Timer(1000, event -> tickTraversalTimer());
        private final Timer campaignCelebrationTimer = new Timer(1350, event -> {
            ((Timer) event.getSource()).stop();
            showOutcomeDialog("Campaign Complete", "BST Champion!",
                    "You found every treasure and earned " + (campaignLevel * 10 + 10)
                            * difficulty.multiplier + campaignBonus
                            + " points on this level. Wonderful work!",
                    true, "Celebrate");
        });
        private TraversalOrder selectedTraversalOrder = TraversalOrder.INORDER;

        GameWindow() {
            super("BST Adventure");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setIconImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
            setMinimumSize(new Dimension(600, 480));
            setSize(980, 760);
            setLocationRelativeTo(null);
            getContentPane().setBackground(BACKGROUND);

            screens.setBackground(BACKGROUND);
            addScreen(createHomeScreen(), "home");
            addScreen(createCampaignScreen(), "campaign");
            addScreen(createPracticeScreen(), "practice");
            addScreen(createTraversalScreen(), "traversal");
            addScreen(createStartScreen(), "start");
            addScreen(createSettingsScreen(), "settings");
            addScreen(createHowToPlayScreen(), "howToPlay");
            addScreen(createMoreGamesScreen(), "moreGames");
            addScreen(createArrangeGameScreen(), "arrange");
            addScreen(createPlacementGameScreen(), "placement");
            addScreen(createDrawTraceScreen(), "drawTrace");
            setContentPane(screens);
            showHome();
        }

        private void addScreen(JPanel screen, String name) {
            JScrollPane scroll = new JScrollPane(screen,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setBackground(BACKGROUND);
            scroll.getViewport().setBackground(BACKGROUND);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            screens.add(scroll, name);
        }

        private JPanel createHomeScreen() {
            ScrollablePage page = menuPage();
            page.add(Box.createVerticalGlue());
            page.add(header("BST ADVENTURE", "Learn the rules. Find the treasure."));
            JLabel themeTag = label("A WOODLAND JOURNEY THROUGH BINARY SEARCH TREES", 11, ACCENT);
            themeTag.setFont(BODY_FONT.deriveFont(Font.BOLD, 11f));
            themeTag.setAlignmentX(CENTER_ALIGNMENT);
            page.add(themeTag);
            page.add(Box.createVerticalStrut(10));
            AdventureBanner banner = new AdventureBanner();
            banner.setAlignmentX(CENTER_ALIGNMENT);
            page.add(banner);
            page.add(Box.createVerticalStrut(20));

            JPanel content = cardPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(20, 36, 20, 36));
            content.setMaximumSize(new Dimension(480, 350));
            content.setAlignmentX(CENTER_ALIGNMENT);
            JLabel welcome = label("Welcome, explorer!", 25, TEXT);
            welcome.setFont(new Font("Serif", Font.BOLD, 25));
            welcome.setAlignmentX(CENTER_ALIGNMENT);
            content.add(welcome);
            content.add(Box.createVerticalStrut(8));
            JLabel rule = label("Smaller values go left. Larger values go right.", 15, MUTED);
            rule.setAlignmentX(CENTER_ALIGNMENT);
            content.add(rule);
            content.add(Box.createVerticalStrut(18));

            JButton start = primaryButton("Start Game");
            start.setIcon(new MenuIcon(MenuIcon.Kind.PLAY));
            start.setIconTextGap(12);
            start.setPreferredSize(new Dimension(280, 54));
            start.setMaximumSize(new Dimension(280, 54));
            start.addActionListener(event -> showStartScreen());
            JButton howToPlayButton = menuSecondaryButton("How to Play");
            howToPlayButton.setIcon(new MenuIcon(MenuIcon.Kind.CHECK));
            howToPlayButton.setIconTextGap(12);
            howToPlayButton.setPreferredSize(new Dimension(280, 46));
            howToPlayButton.setMaximumSize(new Dimension(280, 46));
            howToPlayButton.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
            howToPlayButton.setForeground(ACCENT);
            howToPlayButton.setBackground(new Color(234, 244, 227));
            howToPlayButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(177, 205, 168), 1),
                    BorderFactory.createEmptyBorder(10, 16, 10, 16)));
            howToPlayButton.setContentAreaFilled(true);
            howToPlayButton.setFocusPainted(false);
            howToPlayButton.addActionListener(event -> showHowToPlay());
            JButton settings = menuSecondaryButton("Settings");
            settings.setIcon(new MenuIcon(MenuIcon.Kind.SETTINGS));
            settings.setIconTextGap(12);
            settings.setPreferredSize(new Dimension(280, 48));
            settings.setMaximumSize(new Dimension(280, 48));
            settings.addActionListener(event -> showSettings());
            JButton moreGames = menuSecondaryButton("More Games");
            moreGames.setIcon(new MenuIcon(MenuIcon.Kind.GAMES));
            moreGames.setIconTextGap(12);
            moreGames.setPreferredSize(new Dimension(280, 48));
            moreGames.setMaximumSize(new Dimension(280, 48));
            moreGames.addActionListener(event -> cards.show(screens, "moreGames"));
            content.add(start);
            content.add(Box.createVerticalStrut(10));
            content.add(howToPlayButton);
            content.add(Box.createVerticalStrut(10));
            content.add(settings);
            content.add(Box.createVerticalStrut(10));
            content.add(moreGames);
            JPanel menuLayout = new JPanel(new java.awt.BorderLayout(18, 0));
            menuLayout.setOpaque(false);
            menuLayout.setMaximumSize(new Dimension(620, 360));
            menuLayout.setAlignmentX(CENTER_ALIGNMENT);
            menuLayout.add(content, java.awt.BorderLayout.CENTER);
            page.add(menuLayout);
            page.add(Box.createVerticalStrut(12));
            page.add(createLeaderboardCard());
            page.add(Box.createVerticalGlue());
            return page;
        }

        private JPanel createStartScreen() {
            ScrollablePage page = menuPage();
            page.add(header("CHOOSE TRAVERSAL", "Select an order before starting the game."));

            JPanel content = cardPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

            JPanel choices = new JPanel();
            choices.setOpaque(false);
            prepareTraversalChoice(inorderChoice, TraversalOrder.INORDER);
            prepareTraversalChoice(preorderChoice, TraversalOrder.PREORDER);
            prepareTraversalChoice(postorderChoice, TraversalOrder.POSTORDER);
            inorderChoice.setPreferredSize(new Dimension(150, 44));
            preorderChoice.setPreferredSize(new Dimension(150, 44));
            postorderChoice.setPreferredSize(new Dimension(150, 44));
            inorderChoice.addActionListener(event -> selectStartOrder(TraversalOrder.INORDER));
            preorderChoice.addActionListener(event -> selectStartOrder(TraversalOrder.PREORDER));
            postorderChoice.addActionListener(event -> selectStartOrder(TraversalOrder.POSTORDER));
            updateStartOrderChoiceButtons();
            choices.add(inorderChoice);
            choices.add(preorderChoice);
            choices.add(postorderChoice);
            content.add(choices);

            traversalRule.setFont(BODY_FONT.deriveFont(Font.BOLD, 16f));
            traversalRule.setForeground(ACCENT);
            traversalRule.setAlignmentX(CENTER_ALIGNMENT);
            content.add(traversalRule);
            startOrderStatus.setForeground(MUTED);
            startOrderStatus.setAlignmentX(CENTER_ALIGNMENT);
            content.add(Box.createVerticalStrut(8));
            content.add(startOrderStatus);
            content.add(Box.createVerticalStrut(22));

            JButton begin = primaryButton("Start Game");
            begin.addActionListener(event -> beginSelectedTraversalGame());
            content.add(begin);

            content.setMaximumSize(new Dimension(560, 320));
            page.add(Box.createVerticalGlue());
            page.add(content);
            addNavigationFooter(page, "Back to Menu", () -> showHome());
            return page;
        }

        private JPanel createSettingsScreen() {
            ScrollablePage page = menuPage();
            page.add(header("SETTINGS", "Adjust game settings and open practice activities."));

            JPanel content = cardPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
            content.setMaximumSize(new Dimension(540, 620));
            content.setAlignmentX(CENTER_ALIGNMENT);
            JLabel difficultyLabel = label("DIFFICULTY", 13, ACCENT);
            difficultyLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 13f));
            difficultyLabel.setAlignmentX(CENTER_ALIGNMENT);
            content.add(difficultyLabel);
            content.add(Box.createVerticalStrut(8));
            styleCombo(difficultyChoice);
            difficultyChoice.setSelectedItem(difficulty);
            difficultyChoice.setAlignmentX(CENTER_ALIGNMENT);
            content.add(difficultyChoice);
            content.add(Box.createVerticalStrut(18));
            JLabel modeLabel = label("CAMPAIGN MODE", 13, ACCENT);
            modeLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 13f));
            modeLabel.setAlignmentX(CENTER_ALIGNMENT);
            content.add(modeLabel);
            content.add(Box.createVerticalStrut(8));
            styleCombo(modeChoice);
            modeChoice.setSelectedItem(campaignMode);
            modeChoice.setAlignmentX(CENTER_ALIGNMENT);
            content.add(modeChoice);
            content.add(Box.createVerticalStrut(10));
            JLabel modeHelp = label("Practice shows sorted values. Challenge hides the sorted hint.",
                    14, MUTED);
            modeHelp.setAlignmentX(CENTER_ALIGNMENT);
            content.add(modeHelp);
            content.add(Box.createVerticalStrut(18));
            JLabel displayLabel = label("DISPLAY & ACCESSIBILITY", 13, ACCENT);
            displayLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 13f));
            displayLabel.setAlignmentX(CENTER_ALIGNMENT);
            content.add(displayLabel);
            content.add(Box.createVerticalStrut(5));
            stylePreferenceChoice(largeVisualsChoice);
            largeVisualsChoice.setSelected(largeTreeVisuals);
            largeVisualsChoice.setAlignmentX(CENTER_ALIGNMENT);
            content.add(largeVisualsChoice);
            stylePreferenceChoice(reducedMotionChoice);
            reducedMotionChoice.setSelected(reducedMotion);
            reducedMotionChoice.setAlignmentX(CENTER_ALIGNMENT);
            content.add(reducedMotionChoice);
            stylePreferenceChoice(soundEffectsChoice);
            soundEffectsChoice.setSelected(soundEnabled);
            soundEffectsChoice.setAlignmentX(CENTER_ALIGNMENT);
            content.add(soundEffectsChoice);
            content.add(Box.createVerticalStrut(12));
            JSeparator sectionDivider = new JSeparator();
            sectionDivider.setForeground(BORDER);
            sectionDivider.setMaximumSize(new Dimension(440, 1));
            content.add(sectionDivider);
            content.add(Box.createVerticalStrut(16));
            JButton save = primaryButton("Save Settings");
            save.setIcon(new MenuIcon(MenuIcon.Kind.CHECK));
            save.setIconTextGap(10);
            setMenuButtonSize(save);
            save.addActionListener(event -> {
                campaignMode = (CampaignMode) modeChoice.getSelectedItem();
                difficulty = (Difficulty) difficultyChoice.getSelectedItem();
                soundEnabled = soundEffectsChoice.isSelected();
                applyDisplayOptions();
                showHome();
            });
            content.add(save);
            content.add(Box.createVerticalStrut(12));
            JButton practice = menuSecondaryButton("Open Practice Lab");
            practice.setIcon(new MenuIcon(MenuIcon.Kind.SEARCH));
            practice.setIconTextGap(10);
            setMenuButtonSize(practice);
            practice.addActionListener(event -> showPractice());
            content.add(practice);
            content.add(Box.createVerticalStrut(10));
            JButton campaign = menuSecondaryButton("Play Level Campaign");
            campaign.setIcon(new MenuIcon(MenuIcon.Kind.TRAIL));
            campaign.setIconTextGap(10);
            setMenuButtonSize(campaign);
            campaign.addActionListener(event -> {
                campaignMode = (CampaignMode) modeChoice.getSelectedItem();
                difficulty = (Difficulty) difficultyChoice.getSelectedItem();
                applyDisplayOptions();
                startCampaign();
            });
            content.add(campaign);
            content.add(Box.createVerticalStrut(10));
            JButton score = menuSecondaryButton("View Score");
            score.setIcon(new MenuIcon(MenuIcon.Kind.TREASURE));
            score.setIconTextGap(10);
            setMenuButtonSize(score);
            score.addActionListener(event -> {
                String leaderboard = makeHighScoreSummary();
                JOptionPane.showMessageDialog(this,
                        "Current score: " + totalScore + "\nHigh scores:\n" + leaderboard
                                + "Best streak: " + bestStreak,
                        "Your Score", JOptionPane.INFORMATION_MESSAGE);
            });
            content.add(score);
            content.add(Box.createVerticalStrut(8));
            JLabel displayHelp = label("Visual preferences apply to all tree activities.", 12, MUTED);
            displayHelp.setAlignmentX(CENTER_ALIGNMENT);
            content.add(displayHelp);
            page.add(Box.createVerticalGlue());
            page.add(content);
            addNavigationFooter(page, "Back to Menu", () -> showHome());
            return page;
        }

        private JPanel createMoreGamesScreen() {
            ScrollablePage page = menuPage();
            page.add(header("MORE GAMES", "Choose a challenge and keep exploring."));

            JPanel content = cardPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
            content.setMaximumSize(new Dimension(590, 520));
            content.setAlignmentX(CENTER_ALIGNMENT);

            JLabel prompt = label("Choose a mini-game:", 17, TEXT);
            prompt.setAlignmentX(CENTER_ALIGNMENT);
            content.add(prompt);
            content.add(Box.createVerticalStrut(12));

            JTextArea howToPlay = new JTextArea(
                    "How to play all games:\n"
                            + "• Start Game: choose a traversal order (In-order, Pre-order, or Post-order), then follow the tree rules to collect treasure.\n"
                            + "• Arrange the Values: pick a traversal order and type the next value in sequence before your hearts run out.\n"
                            + "• Trace the Sorted Values: click nodes in the correct order before the timer ends.\n"
                            + "• Choose Left or Right: decide whether each value belongs on the left or right side of the BST.\n"
                            + "Tip: smaller values go left and larger values go right.");
            howToPlay.setFont(BODY_FONT.deriveFont(Font.PLAIN, 13f));
            howToPlay.setForeground(MUTED);
            howToPlay.setOpaque(false);
            howToPlay.setEditable(false);
            howToPlay.setFocusable(false);
            howToPlay.setLineWrap(true);
            howToPlay.setWrapStyleWord(true);
            howToPlay.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
            howToPlay.setMaximumSize(new Dimension(520, 150));
            howToPlay.setAlignmentX(CENTER_ALIGNMENT);
            content.add(howToPlay);
            content.add(Box.createVerticalStrut(12));
            content.add(miniGameChoice("Arrange the Values",
                    "Enter values in the chosen traversal order.", "Play",
                    () -> openArrangeGame()));
            content.add(Box.createVerticalStrut(10));
            content.add(miniGameChoice("Trace the Sorted Values",
                    "Click nodes in traversal order before time runs out.", "Play",
                    () -> startDrawTraceGame()));
            content.add(Box.createVerticalStrut(10));
            content.add(miniGameChoice("Choose Left or Right",
                    "Choose left or right to build a BST.", "Play",
                    () -> startPlacementGame()));
            page.add(Box.createVerticalGlue());
            page.add(content);
            addNavigationFooter(page, "Back to Menu", () -> showHome());
            return page;
        }

        private JPanel miniGameChoice(String title, String description, String action,
                Runnable onSelect) {
            JPanel choice = new JPanel(new BorderLayout(14, 0));
            choice.setBackground(new Color(250, 248, 235));
            choice.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(222, 213, 179)),
                            BorderFactory.createMatteBorder(0, 4, 0, 0, ACCENT)),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            choice.setMaximumSize(new Dimension(520, 98));
            choice.setMinimumSize(new Dimension(0, 98));
            choice.setPreferredSize(new Dimension(520, 98));
            choice.setAlignmentX(CENTER_ALIGNMENT);

            MiniGameIcon icon = new MiniGameIcon(title);
            choice.add(icon, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel titleLabel = label(title, 16, ACCENT);
            titleLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 16f));
            titleLabel.setAlignmentX(LEFT_ALIGNMENT);
            JTextArea descriptionLabel = new JTextArea(description);
            descriptionLabel.setFont(BODY_FONT.deriveFont(13f));
            descriptionLabel.setForeground(MUTED);
            descriptionLabel.setOpaque(false);
            descriptionLabel.setEditable(false);
            descriptionLabel.setFocusable(false);
            descriptionLabel.setLineWrap(true);
            descriptionLabel.setWrapStyleWord(true);
            descriptionLabel.setRows(2);
            descriptionLabel.setMinimumSize(new Dimension(0, 34));
            descriptionLabel.setMaximumSize(new Dimension(360, 38));
            descriptionLabel.setBorder(BorderFactory.createEmptyBorder());
            descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
            text.add(titleLabel);
            text.add(Box.createVerticalStrut(4));
            text.add(descriptionLabel);

            JButton play = button(action);
            play.setPreferredSize(new Dimension(96, 42));
            play.setToolTipText("Start " + title);
            play.getAccessibleContext().setAccessibleDescription("Start " + title);
            play.addActionListener(event -> onSelect.run());
            choice.add(text, BorderLayout.CENTER);
            choice.add(play, BorderLayout.EAST);
            return choice;
        }

        private JPanel createArrangeGameScreen() {
            JPanel page = page(ScreenScene.MINI_GAME);
            page.add(header("ARRANGE THE VALUES", "Choose an order, then enter the values in that traversal."));

            arrangeTree.setPreferredSize(new Dimension(700, 310));
            arrangeTree.setMinimumSize(new Dimension(400, 220));
            page.add(arrangeTree);

            JPanel content = cardPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
            JLabel orderPrompt = label("Arrange in:", 10, MUTED);
            orderPrompt.setAlignmentX(CENTER_ALIGNMENT);
            content.add(orderPrompt);
            ButtonGroup arrangeOrderGroup = new ButtonGroup();
            JRadioButton[] arrangeChoices = {
                    arrangeInorderChoice, arrangePreorderChoice, arrangePostorderChoice
            };
            for (JRadioButton choice : arrangeChoices) {
                styleSelectionChoice(choice);
                arrangeOrderGroup.add(choice);
            }
            arrangeInorderChoice.setSelected(true);
            JPanel arrangeOrderOptions = new JPanel();
            arrangeOrderOptions.setOpaque(false);
            arrangeOrderOptions.add(arrangeInorderChoice);
            arrangeOrderOptions.add(arrangePreorderChoice);
            arrangeOrderOptions.add(arrangePostorderChoice);
            content.add(arrangeOrderOptions);
            content.add(Box.createVerticalStrut(4));
            arrangeValuesLabel.setForeground(ACCENT);
            arrangeValuesLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 17f));
            arrangeValuesLabel.setAlignmentX(CENTER_ALIGNMENT);
            content.add(arrangeValuesLabel);
            styleProgress(arrangeProgress, "Traversal progress");
            content.add(arrangeProgress);
            content.add(Box.createVerticalStrut(10));
            JLabel instruction = label("Type the next value in the selected traversal and submit.", 14, MUTED);
            instruction.setAlignmentX(CENTER_ALIGNMENT);
            content.add(instruction);
            JPanel inputRow = new JPanel();
            inputRow.setOpaque(false);
            arrangeValueInput.setFont(BODY_FONT);
            arrangeValueInput.setHorizontalAlignment(JTextField.CENTER);
            arrangeValueInput.setEnabled(false);
            arrangeValueInput.addActionListener(event -> submitArrangeValue());
            inputRow.add(arrangeValueInput);
            JButton submit = button("Submit");
            submit.addActionListener(event -> submitArrangeValue());
            inputRow.add(submit);
            JButton newRound = button("Start / New Round");
            newRound.addActionListener(event -> startArrangeGame());
            inputRow.add(newRound);
            content.add(inputRow);
            arrangeHeartsLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 15f));
            arrangeHeartsLabel.setForeground(new Color(174, 57, 49));
            arrangeHeartsLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(arrangeHeartsLabel, FEEDBACK_ERROR);
            content.add(arrangeHeartsLabel);
            arrangeStatusLabel.setForeground(TEXT);
            arrangeStatusLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(arrangeStatusLabel, MUTED);
            content.add(arrangeStatusLabel);
            page.add(content);
            addNavigationFooter(page, "Back to More Games", () -> cards.show(screens, "moreGames"));
            return page;
        }

        private JPanel createDrawTraceScreen() {
            JPanel page = page(ScreenScene.MINI_GAME);
            page.add(header("TRACE THE SORTED VALUES",
                    "Draw the selected traversal path by clicking each node in sequence."));

            drawTraceTree.setPreferredSize(new Dimension(700, 340));
            drawTraceTree.setMinimumSize(new Dimension(400, 230));
            styleProgress(drawTraceProgress, "Nodes traced");
            drawTraceTree.setNodeClickListener(node -> handleDrawTraceClick(node));
            page.add(drawTraceTree);

            JPanel controls = cardPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            controls.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
            drawTraceRuleLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 15f));
            drawTraceRuleLabel.setForeground(ACCENT);
            drawTraceRuleLabel.setAlignmentX(CENTER_ALIGNMENT);
            controls.add(drawTraceRuleLabel);
            JPanel traceOrderRow = new JPanel();
            traceOrderRow.setOpaque(false);
            ButtonGroup traceOrderGroup = new ButtonGroup();
            JRadioButton[] traceChoices = {
                    traceInorderChoice, tracePreorderChoice, tracePostorderChoice
            };
            for (JRadioButton choice : traceChoices) {
                styleSelectionChoice(choice);
                traceOrderGroup.add(choice);
                traceOrderRow.add(choice);
            }
            traceInorderChoice.setSelected(true);
            for (JRadioButton choice : traceChoices) {
                choice.addActionListener(event -> updateDrawTraceRule());
            }
            controls.add(traceOrderRow);
            JPanel actions = new JPanel();
            actions.setOpaque(false);
            JButton start = button("Start Trace");
            start.addActionListener(event -> resetDrawTrace());
            actions.add(start);
            JButton restart = button("Restart Trace");
            restart.addActionListener(event -> resetDrawTrace());
            actions.add(restart);
            controls.add(actions);
            controls.add(drawTraceProgress);
            drawTraceStatus.setForeground(TEXT);
            drawTraceStatus.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(drawTraceStatus, MUTED);
            controls.add(drawTraceStatus);
            drawTraceTimerLabel.setForeground(ACCENT);
            drawTraceTimerLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 16f));
            drawTraceTimerLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(drawTraceTimerLabel, ACCENT);
            controls.add(drawTraceTimerLabel);
            drawTraceOutput.setForeground(MUTED);
            drawTraceOutput.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(drawTraceOutput, MUTED);
            controls.add(drawTraceOutput);
            page.add(controls);
            addNavigationFooter(page, "Back to More Games", () -> {
                drawTraceActive = false;
                drawTraceTimer.stop();
                cards.show(screens, "moreGames");
            });
            return page;
        }

        private JPanel createPlacementGameScreen() {
            JPanel page = page(ScreenScene.MINI_GAME);
            page.add(header("LEFT OR RIGHT?", "Place each new value using the BST rule."));

            placementTree.setPreferredSize(new Dimension(700, 300));
            placementTree.setMinimumSize(new Dimension(400, 220));
            styleProgress(placementProgress, "Round progress");
            placementProgress.setMaximum(10);
            page.add(placementTree);

            JPanel content = cardPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
            placementQuestionLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 20f));
            placementQuestionLabel.setForeground(ACCENT);
            placementQuestionLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(placementQuestionLabel, ACCENT);
            content.add(placementQuestionLabel);
            content.add(placementProgress);
            JPanel choices = new JPanel();
            choices.setOpaque(false);
            placementLeftButton.addActionListener(event -> answerPlacement(true));
            placementRightButton.addActionListener(event -> answerPlacement(false));
            placementNextLevelButton.addActionListener(event -> startPlacementNextLevel());
            placementNextLevelButton.setVisible(false);
            choices.add(placementLeftButton);
            choices.add(placementRightButton);
            choices.add(placementNextLevelButton);
            content.add(choices);
            placementHeartsLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 17f));
            placementHeartsLabel.setForeground(new Color(174, 57, 49));
            placementHeartsLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(placementHeartsLabel, FEEDBACK_ERROR);
            content.add(placementHeartsLabel);
            placementStatusLabel.setForeground(TEXT);
            placementStatusLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(placementStatusLabel, MUTED);
            content.add(placementStatusLabel);
            addNavigationFooter(page, "Back to More Games", () -> {
                placementAdvanceTimer.stop();
                cards.show(screens, "moreGames");
            });
            page.add(content);
            return page;
        }

        private JPanel createCampaignScreen() {
            JPanel page = page(ScreenScene.CAMPAIGN);
            page.add(header("LEVEL CAMPAIGN", "Follow the Binary Search Tree to the target."));

            JPanel info = cardPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            campaignTitle.setFont(BODY_FONT.deriveFont(Font.BOLD, 18f));
            campaignTitle.setForeground(TEXT);
            targetLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 17f));
            targetLabel.setForeground(ACCENT);
            mistakesLabel.setForeground(MUTED);
            styleStatusBadge(campaignTitle, TEXT);
            styleStatusBadge(targetLabel, ACCENT);
            styleStatusBadge(mistakesLabel, MUTED);
            info.add(campaignTitle);
            info.add(Box.createVerticalStrut(5));
            campaignTrail.setAlignmentX(CENTER_ALIGNMENT);
            info.add(campaignTrail);
            info.add(Box.createVerticalStrut(8));
            info.add(targetLabel);
            info.add(mistakesLabel);
            page.add(info);

            campaignTree.setPreferredSize(new Dimension(700, 340));
            campaignTree.setMinimumSize(new Dimension(400, 250));
            page.add(campaignTree);

            JPanel controls = cardPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            controls.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            feedbackLabel.setForeground(TEXT);
            feedbackLabel.setAlignmentX(CENTER_ALIGNMENT);
            feedbackLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 15f));
            styleStatusBadge(feedbackLabel, MUTED);
            controls.add(feedbackLabel);
            updateStreakLabel();
            styleStatusBadge(streakLabel, ACCENT);
            streakLabel.setAlignmentX(CENTER_ALIGNMENT);
            controls.add(streakLabel);
            controls.add(Box.createVerticalStrut(8));
            JButton hintButton = menuSecondaryButton("Hint");
            hintButton.addActionListener(event -> showCampaignHint());
            hintButton.setPreferredSize(new Dimension(180, 42));
            hintButton.setMaximumSize(new Dimension(180, 42));
            controls.add(hintButton);
            controls.add(Box.createVerticalStrut(12));
            JPanel directions = new JPanel();
            directions.setOpaque(false);
            styleButton(leftButton);
            styleButton(rightButton);
            leftButton.addActionListener(event -> makeMove(true));
            rightButton.addActionListener(event -> makeMove(false));
            directions.add(leftButton);
            directions.add(rightButton);
            controls.add(directions);
            advanceButton.addActionListener(event -> advanceCampaign());
            styleButton(advanceButton);
            advanceButton.setAlignmentX(CENTER_ALIGNMENT);
            controls.add(advanceButton);
            page.add(controls);
            addNavigationFooter(page, "Back to Menu", () -> showHome());
            return page;
        }

        private JPanel createPracticeScreen() {
            JPanel page = page(ScreenScene.PRACTICE);
            page.add(header("BST PRACTICE LAB", "Build a tree by inserting and searching values."));
            practiceTree.setPreferredSize(new Dimension(700, 330));
            practiceTree.setMinimumSize(new Dimension(400, 230));
            page.add(practiceTree);

            JPanel controls = cardPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            controls.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            practiceValues.setForeground(MUTED);
            practiceValues.setAlignmentX(CENTER_ALIGNMENT);
            practiceMessage.setForeground(TEXT);
            practiceMessage.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(practiceMessage, MUTED);
            controls.add(practiceValues);
            controls.add(Box.createVerticalStrut(8));
            controls.add(practiceMessage);
            controls.add(Box.createVerticalStrut(10));

            JPanel actions = new JPanel();
            actions.setOpaque(false);
            practiceInput.setFont(BODY_FONT);
            practiceInput.setForeground(TEXT);
            practiceInput.setBackground(Color.WHITE);
            practiceInput.setCaretColor(ACCENT);
            practiceInput.setSelectionColor(new Color(190, 221, 193));
            practiceInput.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            actions.add(practiceInput);
            JButton insert = button("Insert");
            insert.addActionListener(event -> practiceAction(true));
            JButton search = button("Search");
            search.addActionListener(event -> practiceAction(false));
            actions.add(insert);
            actions.add(search);
            controls.add(actions);
            JButton traversal = button("Choose Traversal Order");
            traversal.setAlignmentX(CENTER_ALIGNMENT);
            traversal.addActionListener(event -> showTraversalTrainer(selectedTraversalOrder));
            controls.add(Box.createVerticalStrut(14));
            controls.add(traversal);
            controls.add(Box.createVerticalStrut(12));
            page.add(controls);
            addNavigationFooter(page, "Back to Menu", () -> showHome());
            return page;
        }

        private JPanel createTraversalScreen() {
            JPanel page = page(ScreenScene.TRAVERSAL);
            page.add(header("TRAVERSAL ORDER TRAINER",
                    "Enter values in the selected traversal order."));

            traversalLevelLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 18f));
            traversalLevelLabel.setForeground(ACCENT);
            traversalLevelLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(traversalLevelLabel, ACCENT);
            page.add(traversalLevelLabel);
            page.add(Box.createVerticalStrut(8));
            traversalTree.setPreferredSize(new Dimension(700, 330));
            traversalTree.setMinimumSize(new Dimension(400, 230));
            page.add(traversalTree);

            JPanel controls = cardPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            controls.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

            traversalRule.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
            traversalRule.setForeground(ACCENT);
            traversalRule.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(traversalRule, ACCENT);
            controls.add(traversalRule);
            styleProgress(traversalProgress, "Traversal progress");
            controls.add(traversalProgress);
            controls.add(Box.createVerticalStrut(8));

            JPanel traceActions = new JPanel();
            traceActions.setOpaque(false);
            JButton startTraceButton = button("Start Ordering");
            startTraceButton.addActionListener(event -> startTrace());
            traceActions.add(startTraceButton);
            JButton resetOrderButton = button("Reset Order");
            resetOrderButton.addActionListener(event -> resetTraversalRound());
            traceActions.add(resetOrderButton);
            controls.add(traceActions);

            traversalHearts.setFont(BODY_FONT.deriveFont(Font.BOLD, 17f));
            traversalHearts.setForeground(new Color(174, 57, 49));
            traversalHearts.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(traversalHearts, FEEDBACK_ERROR);
            controls.add(traversalHearts);
            updateTraversalHearts();
            traversalTimerLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 16f));
            traversalTimerLabel.setForeground(ACCENT);
            traversalTimerLabel.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(traversalTimerLabel, ACCENT);
            controls.add(traversalTimerLabel);

            JLabel enterLabel = label("Enter the next value in the traversal:", 15, TEXT);
            enterLabel.setAlignmentX(CENTER_ALIGNMENT);
            controls.add(Box.createVerticalStrut(8));
            controls.add(enterLabel);
            JPanel entryActions = new JPanel();
            entryActions.setOpaque(false);
            traversalValueInput.setFont(BODY_FONT);
            traversalValueInput.setForeground(TEXT);
            traversalValueInput.setBackground(Color.WHITE);
            traversalValueInput.setCaretColor(ACCENT);
            traversalValueInput.setHorizontalAlignment(JTextField.CENTER);
            traversalValueInput.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            traversalValueInput.setEnabled(false);
            traversalValueInput.addActionListener(event -> submitTraversalValue());
            entryActions.add(traversalValueInput);
            JButton submitValueButton = button("Submit Value");
            submitValueButton.addActionListener(event -> submitTraversalValue());
            entryActions.add(submitValueButton);
            controls.add(entryActions);

            traceStatus.setForeground(TEXT);
            traceStatus.setAlignmentX(CENTER_ALIGNMENT);
            styleStatusBadge(traceStatus, MUTED);
            controls.add(traceStatus);
            traceOutput.setFont(BODY_FONT);
            traceOutput.setForeground(MUTED);
            traceOutput.setBackground(PANEL);
            traceOutput.setEditable(false);
            traceOutput.setFocusable(false);
            traceOutput.setLineWrap(true);
            traceOutput.setWrapStyleWord(true);
            traceOutput.setText("Your order: (none)");
            traceOutput.setAlignmentX(CENTER_ALIGNMENT);
            traceOutput.setMaximumSize(new Dimension(680, 46));
            controls.add(traceOutput);

            page.add(controls);
            addNavigationFooter(page, "Back to Menu", () -> showHome());
            return page;
        }

        private ScrollablePage page(ScreenScene scene) {
            ScrollablePage panel = new ScrollablePage();
            panel.setBackground(BACKGROUND);
            panel.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 16));
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setScene(scene);
            return panel;
        }

        private ScrollablePage menuPage() {
            return page(ScreenScene.MENU);
        }

        private JPanel header(String title, String subtitle) {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JLabel heading = label(title, 24, ACCENT);
            heading.setFont(HEADING_FONT);
            heading.setAlignmentX(CENTER_ALIGNMENT);
            JLabel subheading = label(subtitle, 14, MUTED);
            subheading.setAlignmentX(CENTER_ALIGNMENT);
            panel.add(heading);
            panel.add(Box.createVerticalStrut(5));
            panel.add(subheading);
            panel.add(new HeaderDivider());
            panel.add(Box.createVerticalStrut(12));
            return panel;
        }

        private void addNavigationFooter(JPanel page, String text, Runnable action) {
            page.add(Box.createVerticalGlue());
            JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
            JButton back = menuSecondaryButton("\u2190  " + text);
            back.setPreferredSize(new Dimension(232, 42));
            back.setMaximumSize(new Dimension(232, 42));
            back.getAccessibleContext().setAccessibleDescription(text);
            back.addActionListener(event -> action.run());
            footer.add(back);
            footer.setAlignmentX(CENTER_ALIGNMENT);
            page.add(footer);
        }

        private void setMenuButtonSize(JButton button) {
            button.setPreferredSize(new Dimension(280, 46));
            button.setMaximumSize(new Dimension(280, 46));
        }

        private JPanel cardPanel() {
            JPanel panel = new RoundedCardPanel();
            panel.setBackground(PANEL);
            panel.setBorder(BorderFactory.createEmptyBorder(17, 18, 17, 18));
            return panel;
        }

        private void styleProgress(JProgressBar progress, String accessibleName) {
            progress.setStringPainted(true);
            progress.setForeground(ACCENT);
            progress.setBackground(new Color(225, 229, 211));
            progress.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            progress.setPreferredSize(new Dimension(460, 24));
            progress.setMaximumSize(new Dimension(560, 26));
            progress.setAlignmentX(CENTER_ALIGNMENT);
            progress.getAccessibleContext().setAccessibleName(accessibleName);
        }

        private void stylePreferenceChoice(JCheckBox choice) {
            choice.setFont(BODY_FONT.deriveFont(Font.PLAIN, 14f));
            choice.setForeground(TEXT);
            choice.setBackground(PANEL);
            choice.setOpaque(false);
            choice.setFocusPainted(true);
            choice.setAlignmentX(CENTER_ALIGNMENT);
        }

        private void applyDisplayOptions() {
            largeTreeVisuals = largeVisualsChoice.isSelected();
            reducedMotion = reducedMotionChoice.isSelected();
            soundEnabled = soundEffectsChoice.isSelected();
            campaignTree.refreshAppearance();
            practiceTree.refreshAppearance();
            traversalTree.refreshAppearance();
            arrangeTree.refreshAppearance();
            placementTree.refreshAppearance();
            drawTraceTree.refreshAppearance();
        }

        private void styleStatusBadge(JLabel label, Color foreground) {
            label.setForeground(foreground);
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setOpaque(false);
            label.setBackground(statusBackground(foreground));
            label.setBorder(new RoundedBorder(statusBorder(foreground), 14, 6, 12));
            label.setMaximumSize(new Dimension(720, 42));
            label.putClientProperty("statusBadge", Boolean.TRUE);
        }

        private Color statusBackground(Color foreground) {
            if (FEEDBACK_ERROR.equals(foreground)) {
                return new Color(250, 239, 235);
            }
            if (WARNING.equals(foreground)) {
                return new Color(250, 246, 229);
            }
            if (SUCCESS.equals(foreground)) {
                return new Color(237, 245, 233);
            }
            if (ACCENT.equals(foreground)) {
                return new Color(238, 244, 233);
            }
            return new Color(249, 248, 239);
        }

        private Color statusBorder(Color foreground) {
            if (FEEDBACK_ERROR.equals(foreground)) {
                return new Color(225, 190, 179);
            }
            if (WARNING.equals(foreground)) {
                return new Color(220, 204, 155);
            }
            if (SUCCESS.equals(foreground) || ACCENT.equals(foreground)) {
                return new Color(192, 211, 177);
            }
            return new Color(220, 215, 192);
        }

        private JLabel label(String text, int size, Color color) {
            JLabel label = new JLabel(text);
            label.setFont(BODY_FONT.deriveFont((float) size));
            label.setForeground(color);
            return label;
        }

        private JButton button(String text) {
            JButton button = new JButton(text);
            styleButton(button);
            return button;
        }

        private JButton primaryButton(String text) {
            JButton button = new JButton(text);
            styleButton(button, true);
            return button;
        }

        private JButton menuSecondaryButton(String text) {
            JButton button = new JButton(text);
            styleButton(button, false, true);
            return button;
        }

        private void styleButton(JButton button) {
            styleButton(button, false, false);
        }

        private void styleButton(JButton button, boolean primary) {
            styleButton(button, primary, false);
        }

        private void styleButton(JButton button, boolean primary, boolean secondary) {
            button.setFont(BODY_FONT.deriveFont(Font.BOLD, primary ? 16f : 14f));
            button.setBackground(menuButtonColor(primary, secondary, false));
            button.setForeground(primary ? TEXT : secondary ? ACCENT : Color.WHITE);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setFocusPainted(false);
            button.setRolloverEnabled(true);
            button.setBorder(buttonBorder(button, primary, secondary));
            if (primary) {
                button.setPreferredSize(new Dimension(220, 50));
            }
            button.addChangeListener(event -> {
                boolean rollover = button.getModel().isRollover() && button.isEnabled();
                button.setBackground(menuButtonColor(primary, secondary, rollover));
                button.setBorder(buttonBorder(button, primary, secondary));
            });
            button.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    button.setBorder(buttonBorder(button, primary, secondary));
                }

                @Override
                public void focusLost(FocusEvent event) {
                    button.setBorder(buttonBorder(button, primary, secondary));
                }
            });
            button.setAlignmentX(CENTER_ALIGNMENT);
        }

        private Color menuButtonColor(boolean primary, boolean secondary, boolean rollover) {
            if (primary) {
                return rollover ? new Color(240, 193, 83) : new Color(229, 181, 69);
            }
            if (secondary) {
                return rollover ? new Color(224, 235, 215) : new Color(255, 252, 241);
            }
            return rollover ? BUTTON_HOVER : BUTTON_COLOR;
        }

        private javax.swing.border.Border buttonBorder(JButton button, boolean primary, boolean secondary) {
            Color outline = button.isFocusOwner() ? new Color(231, 185, 77)
                    : secondary ? new Color(161, 184, 143) : BUTTON_HOVER;
            int thickness = button.isFocusOwner() ? 2 : 1;
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(outline, thickness),
                    BorderFactory.createEmptyBorder(primary ? 12 : 8,
                            primary ? 28 : 16, primary ? 12 : 8, primary ? 28 : 16));
        }

        private void styleCombo(JComboBox<?> combo) {
            combo.setMaximumSize(new Dimension(320, 40));
            combo.setFont(BODY_FONT);
            combo.setForeground(TEXT);
            combo.setBackground(PANEL);
            combo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            combo.setRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    JLabel cell = (JLabel) super.getListCellRendererComponent(
                            list, value, index, isSelected, cellHasFocus);
                    cell.setFont(BODY_FONT);
                    cell.setForeground(isSelected ? Color.WHITE : TEXT);
                    cell.setBackground(isSelected ? ACCENT : PANEL);
                    cell.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                    return cell;
                }
            });
        }

        private void showHome() {
            campaignCelebrationTimer.stop();
            updateScore();
            cards.show(screens, "home");
        }

        private void showSettings() {
            difficultyChoice.setSelectedItem(difficulty);
            modeChoice.setSelectedItem(campaignMode);
            largeVisualsChoice.setSelected(largeTreeVisuals);
            reducedMotionChoice.setSelected(reducedMotion);
            soundEffectsChoice.setSelected(soundEnabled);
            cards.show(screens, "settings");
        }

        private JPanel createLeaderboardCard() {
            JPanel panel = cardPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setMaximumSize(new Dimension(500, 150));
            panel.setAlignmentX(CENTER_ALIGNMENT);
            JLabel title = label("TOP EXPLORERS", 13, ACCENT);
            title.setFont(BODY_FONT.deriveFont(Font.BOLD, 13f));
            title.setAlignmentX(CENTER_ALIGNMENT);
            panel.add(title);
            panel.add(Box.createVerticalStrut(8));
            JTextArea scores = new JTextArea(buildLeaderboardText());
            scores.setEditable(false);
            scores.setFocusable(false);
            scores.setOpaque(false);
            scores.setFont(BODY_FONT.deriveFont(Font.PLAIN, 13f));
            scores.setForeground(MUTED);
            scores.setLineWrap(false);
            scores.setWrapStyleWord(false);
            scores.setAlignmentX(CENTER_ALIGNMENT);
            scores.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            panel.add(scores);
            return panel;
        }

        private String buildLeaderboardText() {
            List<Integer> scores = loadHighScores();
            if (scores.isEmpty()) {
                return "No scores yet. Start your first BST run!";
            }
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < Math.min(scores.size(), 5); i++) {
                text.append("#").append(i + 1).append("  ").append(scores.get(i)).append(" points\n");
            }
            return text.toString();
        }

        private void playTone(float frequency, float durationMs, float volume) {
            if (!soundEnabled) {
                return;
            }
            try {
                int sampleRate = 44100;
                int totalSamples = (int) (sampleRate * durationMs / 1000.0);
                byte[] buffer = new byte[totalSamples * 2];
                double amplitude = volume * 0.8;
                for (int i = 0; i < totalSamples; i++) {
                    double t = i / (double) sampleRate;
                    double wave = Math.sin(2.0 * Math.PI * frequency * t);
                    double envelope = Math.min(1.0, (double) i / (sampleRate * 0.08));
                    short sample = (short) (wave * amplitude * 32767 * Math.max(0.25, 1.0 - (i / (double) totalSamples)));
                    buffer[i * 2] = (byte) (sample & 0xFF);
                    buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                    if (envelope < 1.0) {
                        buffer[i * 2] = (byte) (buffer[i * 2] * envelope);
                        buffer[i * 2 + 1] = (byte) (buffer[i * 2 + 1] * envelope);
                    }
                }
                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                    line.open(format);
                    line.start();
                    line.write(buffer, 0, buffer.length);
                    line.drain();
                    line.stop();
                }
            } catch (Exception ignored) {
            }
        }

        private void playCorrectSound() {
            if (!soundEnabled) {
                return;
            }
            playTone(660f, 160f, 0.6f);
        }

        private void playWrongSound() {
            if (!soundEnabled) {
                return;
            }
            playTone(210f, 220f, 0.45f);
        }

        private void playPerfectRoundSound() {
            if (!soundEnabled) {
                return;
            }
            playTone(520f, 120f, 0.55f);
            playTone(660f, 120f, 0.65f);
            playTone(820f, 180f, 0.7f);
        }

        private List<Integer> loadHighScores() {
            List<Integer> scores = new ArrayList<>();
            if (!Files.exists(HIGH_SCORE_PATH)) {
                return scores;
            }
            try (BufferedReader reader = Files.newBufferedReader(HIGH_SCORE_PATH, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            scores.add(Integer.parseInt(trimmed));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } catch (IOException ignored) {
            }
            scores.sort(Collections.reverseOrder());
            return scores;
        }

        private void saveHighScores(List<Integer> scores) {
            try {
                List<Integer> trimmed = new ArrayList<>(scores);
                trimmed.sort(Collections.reverseOrder());
                while (trimmed.size() > 5) {
                    trimmed.remove(trimmed.size() - 1);
                }
                List<String> lines = new ArrayList<>();
                for (Integer score : trimmed) {
                    lines.add(String.valueOf(score));
                }
                Files.write(HIGH_SCORE_PATH, lines, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }

        private int getBestHighScore() {
            List<Integer> scores = loadHighScores();
            return scores.isEmpty() ? 0 : scores.get(0);
        }

        private void recordHighScoreIfNeeded(int score) {
            if (score <= 0 || score <= getBestHighScore()) {
                return;
            }
            List<Integer> scores = loadHighScores();
            scores.add(score);
            saveHighScores(scores);
        }

        private String makeHighScoreSummary() {
            List<Integer> scores = loadHighScores();
            if (scores.isEmpty()) {
                return "No high scores saved yet.";
            }
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < Math.min(scores.size(), 5); i++) {
                summary.append("#").append(i + 1).append(" - ").append(scores.get(i)).append(" points\n");
            }
            return summary.toString();
        }

        private JPanel createHowToPlayScreen() {
            ScrollablePage page = menuPage();
            page.add(header("HOW TO PLAY", "Learn the BST rules before you begin."));

            JPanel content = cardPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
            content.setMaximumSize(new Dimension(560, 520));
            content.setAlignmentX(CENTER_ALIGNMENT);

            JTextArea guide = new JTextArea(
                    "1. Choose a traversal order: In-order, Pre-order, or Post-order.\n\n"
                            + "2. Insert new values or search for values to grow the tree and earn points.\n\n"
                            + "3. Follow the BST rule: smaller values always go left and larger values always go right.\n\n"
                            + "4. Complete campaign levels and mini-games to collect treasure and unlock more challenges.\n\n"
                            + "Tip: before you guess, check the current tree and follow the branch that matches the value.");
            guide.setFont(BODY_FONT.deriveFont(Font.PLAIN, 14f));
            guide.setForeground(MUTED);
            guide.setOpaque(false);
            guide.setEditable(false);
            guide.setFocusable(false);
            guide.setLineWrap(true);
            guide.setWrapStyleWord(true);
            guide.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            guide.setAlignmentX(CENTER_ALIGNMENT);
            content.add(guide);
            content.add(Box.createVerticalStrut(18));

            JButton start = primaryButton("Start Game");
            start.setPreferredSize(new Dimension(220, 52));
            start.addActionListener(event -> {
                showStartScreen();
            });
            content.add(start);

            page.add(Box.createVerticalGlue());
            page.add(content);
            addNavigationFooter(page, "Back to Menu", () -> showHome());
            return page;
        }

        private void showHowToPlay() {
            cards.show(screens, "howToPlay");
        }

        private void showStartScreen() {
            updateStartOrderChoiceButtons();
            startOrderStatus.setText("Selected: " + selectedTraversalOrder.label
                    + ". Select Start Game when ready.");
            cards.show(screens, "start");
        }

        private void selectStartOrder(TraversalOrder order) {
            selectedTraversalOrder = order;
            traversalRule.setText(order.label + ": " + order.rule);
            startOrderStatus.setText("Selected: " + order.label);
            updateStartOrderChoiceButtons();
        }

        private void updateStartOrderChoiceButtons() {
            styleTraversalChoice(inorderChoice, TraversalOrder.INORDER);
            styleTraversalChoice(preorderChoice, TraversalOrder.PREORDER);
            styleTraversalChoice(postorderChoice, TraversalOrder.POSTORDER);
        }

        private void beginSelectedTraversalGame() {
            traversalLevel = 1;
            traversalRule.setText(selectedTraversalOrder.label + ": " + selectedTraversalOrder.rule);
            cards.show(screens, "traversal");
            beginTraversalLevel();
        }

        private void updateScore() {
            if (totalScore > getBestHighScore()) {
                recordHighScoreIfNeeded(totalScore);
            }
        }

        private void setFeedback(JLabel label, String text, Color color) {
            label.setText(text);
            label.setForeground(color);
            if (Boolean.TRUE.equals(label.getClientProperty("statusBadge"))) {
                label.setBackground(statusBackground(color));
                label.setBorder(new RoundedBorder(statusBorder(color), 14, 6, 12));
            }
        }

        private void updateStreakLabel() {
            streakLabel.setText("Streak: " + currentStreak + " · Best: " + bestStreak);
        }

        private void refreshCampaignProgress() {
            updateScore();
            updateStreakLabel();
        }

        private void awardCorrectMove() {
            currentStreak++;
            if (currentStreak > bestStreak) {
                bestStreak = currentStreak;
            }
            totalScore += 2;
            playCorrectSound();
            if (currentStreak >= 3 && currentStreak % 3 == 0) {
                playPerfectRoundSound();
                showOutcomeDialog("Perfect Round!", "On fire!",
                        "Amazing streak! You made " + currentStreak + " correct moves in a row.",
                        true, "Nice!");
            }
            updateStreakLabel();
            updateScore();
        }

        private void resetCampaignStreak() {
            currentStreak = 0;
            updateStreakLabel();
        }

        private void showCampaignHint() {
            if (campaignEnded || currentNode == null) {
                return;
            }
            boolean shouldGoLeft = target < currentNode.value;
            String direction = shouldGoLeft ? "left" : "right";
            setFeedback(feedbackLabel,
                    "Hint: the target is " + target + ". Since " + target + " is "
                            + (shouldGoLeft ? "smaller" : "larger") + " than " + currentNode.value
                            + ", follow the " + direction + " branch.",
                    WARNING);
        }

        private int showOutcomeDialog(String title, String heading, String message,
                boolean success, String... options) {
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(PANEL);
            content.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            success ? new Color(190, 166, 103) : new Color(202, 156, 130), 2),
                    BorderFactory.createEmptyBorder(18, 26, 18, 26)));

            OutcomeBadge badge = new OutcomeBadge(success);
            badge.setAlignmentX(CENTER_ALIGNMENT);
            content.add(badge);
            content.add(Box.createVerticalStrut(8));

            JLabel headingLabel = label(heading, 22, success ? ACCENT : new Color(137, 82, 53));
            headingLabel.setFont(new Font("Serif", Font.BOLD, 22));
            headingLabel.setAlignmentX(CENTER_ALIGNMENT);
            content.add(headingLabel);
            content.add(Box.createVerticalStrut(6));

            JTextArea messageLabel = new JTextArea(message);
            messageLabel.setFont(BODY_FONT);
            messageLabel.setForeground(TEXT);
            messageLabel.setBackground(PANEL);
            messageLabel.setEditable(false);
            messageLabel.setFocusable(false);
            messageLabel.setLineWrap(true);
            messageLabel.setWrapStyleWord(true);
            messageLabel.setColumns(34);
            messageLabel.setMaximumSize(new Dimension(380, 72));
            messageLabel.setAlignmentX(CENTER_ALIGNMENT);
            content.add(messageLabel);

            return JOptionPane.showOptionDialog(this, content, title,
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options.length == 0 ? null : options[0]);
        }

        private void startCampaign() {
            campaignCelebrationTimer.stop();
            campaignLevel = 1;
            completedLevels = 0;
            campaignBonus = 0;
            currentStreak = 0;
            bestStreak = 0;
            campaignEnded = false;
            campaignTrail.setCompletedLevels(0);
            updateStreakLabel();
            beginLevel();
            cards.show(screens, "campaign");
        }

        private void beginLevel() {
            int nodeCount = campaignLevel + difficulty.extraNodes;
            campaignRoot = createRandomTree(nodeCount, random);
            currentNode = campaignRoot;
            campaignMistakes = 0;
            List<Integer> values = inorderValues(campaignRoot);
            target = values.get(random.nextInt(values.size()));
            campaignTitle.setText("Level " + campaignLevel + " / 5  ·  " + difficulty.label
                    + "  ·  " + nodeCount + " nodes");
            campaignTrail.setCompletedLevels(campaignLevel - 1);
            targetLabel.setText("Find target: " + target);
            String hint = campaignMode.showSortedValues
                    ? "Sorted values: " + values + "   "
                    : "";
            mistakesLabel.setText(hint + "Mistakes allowed: " + difficulty.mistakesAllowed);
            setFeedback(feedbackLabel, "Choose LEFT or RIGHT. Smaller values go left; larger values go right.", MUTED);
            leftButton.setVisible(true);
            rightButton.setVisible(true);
            advanceButton.setVisible(false);
            campaignTree.setTree(campaignRoot, currentNode);
            if (currentNode.value == target) {
                completeLevel();
            }
        }

        private void makeMove(boolean goLeft) {
            if (campaignEnded || currentNode == null) {
                return;
            }
            boolean shouldGoLeft = target < currentNode.value;
            if (goLeft != shouldGoLeft) {
                campaignMistakes++;
                resetCampaignStreak();
                playWrongSound();
                campaignTree.flashNode(currentNode, FEEDBACK_ERROR);
                int remaining = difficulty.mistakesAllowed - campaignMistakes;
                setFeedback(feedbackLabel, remaining <= 0
                        ? "Out of mistakes. Start a new campaign or return to the menu."
                        : "Not quite! The target is " + (shouldGoLeft ? "smaller" : "larger")
                                + ". Mistakes remaining: " + remaining, FEEDBACK_ERROR);
                if (remaining <= 0) {
                    endCampaign(false);
                }
                return;
            }

            currentNode = goLeft ? currentNode.left : currentNode.right;
            campaignTree.setTree(campaignRoot, currentNode);
            if (currentNode == null) {
                resetCampaignStreak();
                setFeedback(feedbackLabel, "That branch is empty. Campaign over; try again!", FEEDBACK_ERROR);
                endCampaign(false);
            } else if (currentNode.value == target) {
                awardCorrectMove();
                campaignTree.flashNode(currentNode, SUCCESS);
                completeLevel();
            } else {
                awardCorrectMove();
                campaignTree.flashNode(currentNode, SUCCESS);
                setFeedback(feedbackLabel, "Correct direction! You are at " + currentNode.value + ".", SUCCESS);
            }
        }

        private void completeLevel() {
            int points = (campaignLevel * 10 + 10) * difficulty.multiplier;
            int streakBonus = currentStreak * 5;
            totalScore += points;
            if (streakBonus > 0) {
                totalScore += streakBonus;
            }
            completedLevels++;
            campaignTrail.setCompletedLevels(completedLevels);
            String rewardMessage = "TREASURE FOUND! Level cleared for +" + points + " points";
            if (streakBonus > 0) {
                rewardMessage += " and +" + streakBonus + " streak bonus";
            }
            setFeedback(feedbackLabel, rewardMessage + ".", SUCCESS);
            leftButton.setVisible(false);
            rightButton.setVisible(false);
            if (campaignLevel == 5) {
                campaignBonus = 50 * difficulty.multiplier;
                totalScore += campaignBonus;
                campaignEnded = true;
                setFeedback(feedbackLabel, "CAMPAIGN COMPLETE! +" + points + " points, +"
                        + streakBonus + " streak bonus, and +" + campaignBonus + " final bonus. BST Champion!", SUCCESS);
                advanceButton.setText("Back to Menu");
                campaignTree.celebratePath(campaignTargetPath());
                campaignCelebrationTimer.setInitialDelay(reducedMotion ? 500 : 1350);
                campaignCelebrationTimer.restart();
            } else {
                advanceButton.setText("Continue to Level " + (campaignLevel + 1));
            }
            advanceButton.setVisible(true);
            refreshCampaignProgress();
        }

        private List<Node> campaignTargetPath() {
            List<Node> path = new ArrayList<>();
            Node node = campaignRoot;
            while (node != null) {
                path.add(node);
                if (node.value == target) {
                    break;
                }
                node = target < node.value ? node.left : node.right;
            }
            return path;
        }

        private void endCampaign(boolean success) {
            campaignEnded = true;
            leftButton.setVisible(false);
            rightButton.setVisible(false);
            advanceButton.setText(success ? "Back to Menu" : "Restart Campaign");
            advanceButton.setVisible(true);
            if (!success) {
                int choice = showOutcomeDialog("Campaign Over", "The trail ends here",
                        "That route did not reach the target. Start a fresh campaign or return to the menu.",
                        false, "Restart Campaign", "Back to Menu");
                if (choice == 0) {
                    startCampaign();
                } else {
                    showHome();
                }
            }
        }

        private void advanceCampaign() {
            if (completedLevels == 5 || campaignEnded) {
                if (completedLevels == 5) {
                    showHome();
                } else {
                    startCampaign();
                }
                return;
            }
            campaignLevel++;
            beginLevel();
        }

        private void showPractice() {
            ensurePracticeTree();
            refreshPracticeTree(null);
            setFeedback(practiceMessage, "Insert values or search the tree.", MUTED);
            practiceInput.setText("");
            clearTrace();
            cards.show(screens, "practice");
        }

        private void showTraversalTrainer(TraversalOrder order) {
            selectedTraversalOrder = order;
            updateStartOrderChoiceButtons();
            clearTrace();
            traversalRule.setText(order.label + ": " + order.rule);
            setFeedback(traceStatus, order.label + " selected. Click Start Ordering, then enter the values.", MUTED);
            cards.show(screens, "traversal");
            traversalLevel = 1;
            beginTraversalLevel();
        }

        private void ensurePracticeTree() {
            if (practiceRoot == null) {
                int[] starterValues = {50, 30, 70, 20, 40, 60, 80};
                for (int value : starterValues) {
                    practiceRoot = insert(practiceRoot, value);
                }
            }
        }

        private void refreshPracticeTree(Node highlighted) {
            practiceTree.setTree(practiceRoot, highlighted);
            practiceValues.setText("Inorder (sorted): " + inorderValues(practiceRoot));
        }

        private void startArrangeGame() {
            arrangeRoot = createRandomTree(7, random);
            TraversalOrder order = selectedArrangeOrder();
            arrangeSortedValues = traversalValues(arrangeRoot, order);
            setArrangeOrderEnabled(false);
            arrangeDisplayedValues = new ArrayList<>(arrangeSortedValues);
            Collections.shuffle(arrangeDisplayedValues, random);
            arrangeIndex = 0;
            arrangeProgress.setMaximum(arrangeSortedValues.size());
            arrangeProgress.setValue(0);
            arrangeProgress.setString("0 / " + arrangeSortedValues.size() + " values");
            arrangeHearts = 3;
            updateArrangeHearts();
            arrangeValuesLabel.setText("Values: " + arrangeDisplayedValues + "    Your order: []");
            setFeedback(arrangeStatusLabel, "Enter the first value in " + order.label + " order.", MUTED);
            arrangeValueInput.setText("");
            arrangeValueInput.setEnabled(true);
            arrangeTree.setTree(arrangeRoot, null);
            arrangeValueInput.requestFocusInWindow();
        }

        private void openArrangeGame() {
            arrangeRoot = null;
            setArrangeOrderEnabled(true);
            arrangeInorderChoice.setSelected(true);
            arrangeValueInput.setText("");
            arrangeValueInput.setEnabled(false);
            arrangeHearts = 3;
            updateArrangeHearts();
            arrangeValuesLabel.setText("Choose an order and start a round.");
            setFeedback(arrangeStatusLabel, "Select In-order, Pre-order, or Post-order.", MUTED);
            arrangeTree.setTree(null, null);
            arrangeProgress.setMaximum(1);
            arrangeProgress.setValue(0);
            arrangeProgress.setString("Start a round to begin");
            cards.show(screens, "arrange");
        }

        private void submitArrangeValue() {
            if (arrangeIndex >= arrangeSortedValues.size()) {
                return;
            }
            int enteredValue;
            try {
                enteredValue = Integer.parseInt(arrangeValueInput.getText().trim());
            } catch (NumberFormatException exception) {
                handleArrangeMistake("Enter a whole number from the displayed values.");
                return;
            }
            int expectedValue = arrangeSortedValues.get(arrangeIndex);
            if (enteredValue != expectedValue) {
                TraversalOrder order = selectedArrangeOrder();
                arrangeTree.flashNode(findNode(arrangeRoot, expectedValue), FEEDBACK_ERROR);
                handleArrangeMistake("Not quite. Follow the selected " + order.label + " traversal.");
                return;
            }

            arrangeIndex++;
            arrangeProgress.setValue(arrangeIndex);
            arrangeProgress.setString(arrangeIndex + " / " + arrangeSortedValues.size() + " values");
            arrangeValueInput.setText("");
            arrangeValuesLabel.setText("Values: " + arrangeDisplayedValues + "    Your order: "
                    + arrangeSortedValues.subList(0, arrangeIndex));
            arrangeTree.setTree(arrangeRoot, findNode(arrangeRoot, expectedValue));
            if (arrangeIndex == arrangeSortedValues.size()) {
                arrangeValueInput.setEnabled(false);
                setArrangeOrderEnabled(true);
                setFeedback(arrangeStatusLabel, "Great job! You arranged all the values correctly.", SUCCESS);
                totalScore += 10;
                updateScore();
                int choice = showOutcomeDialog("Round Complete", "Excellent work!",
                        "You arranged every value in " + selectedArrangeOrder().label
                                + " order and earned 10 points. Play another round?",
                        true, "Play Another Round", "Back to More Games");
                if (choice == 0) {
                    startArrangeGame();
                } else {
                    cards.show(screens, "moreGames");
                }
            } else {
                setFeedback(arrangeStatusLabel, "Correct! " + arrangeIndex + " of "
                        + arrangeSortedValues.size() + " values. Enter the next traversal value.", SUCCESS);
            }
        }

        private TraversalOrder selectedArrangeOrder() {
            if (arrangePreorderChoice.isSelected()) {
                return TraversalOrder.PREORDER;
            }
            if (arrangePostorderChoice.isSelected()) {
                return TraversalOrder.POSTORDER;
            }
            return TraversalOrder.INORDER;
        }

        private void setArrangeOrderEnabled(boolean enabled) {
            arrangeInorderChoice.setEnabled(enabled);
            arrangePreorderChoice.setEnabled(enabled);
            arrangePostorderChoice.setEnabled(enabled);
        }

        private void updateArrangeHearts() {
            arrangeHeartsLabel.setLives(arrangeHearts);
        }

        private void handleArrangeMistake(String message) {
            arrangeHearts--;
            updateArrangeHearts();
            arrangeValueInput.selectAll();
            if (arrangeHearts > 0) {
                setFeedback(arrangeStatusLabel, message + " " + arrangeHearts + " life(s) left.", FEEDBACK_ERROR);
                return;
            }

            arrangeValueInput.setEnabled(false);
            setArrangeOrderEnabled(true);
            String[] options = {"Restart Game", "Back to More Games"};
            int choice = showOutcomeDialog("Round Over", "Take a breather",
                    "You used all three lives. Start a new round or choose another mini-game.",
                    false, options);
            if (choice == 0) {
                startArrangeGame();
            } else {
                cards.show(screens, "moreGames");
            }
        }

        private void startPlacementGame() {
            placementAdvanceTimer.stop();
            placementRound = 1;
            placementLevel = 1;
            placementHearts = 3;
            placementRoot = new Node(random.nextInt(80) + 10);
            beginPlacementQuestion();
            cards.show(screens, "placement");
        }

        private void beginPlacementQuestion() {
            placementAdvanceTimer.stop();
            do {
                placementValue = random.nextInt(90) + 10;
            } while (search(placementRoot, placementValue));
            placementCurrentNode = placementRoot;
            placementParentValue = placementCurrentNode.value;
            placementAnswerPending = true;
            placementQuestionLabel.setText("Level " + placementLevel + "  |  Round "
                    + placementRound + " / 10: Where does "
                    + placementValue + " go compared with " + placementParentValue + "?");
            placementProgress.setValue(placementRound - 1);
            placementProgress.setString("Level " + placementLevel + " · " + (placementRound - 1) + " / 10");
            setFeedback(placementStatusLabel, "Smaller values go left; larger values go right.", MUTED);
            placementLeftButton.setEnabled(true);
            placementRightButton.setEnabled(true);
            placementNextLevelButton.setVisible(false);
            updatePlacementHearts();
            placementTree.setTree(placementRoot, placementRoot);
        }

        private void answerPlacement(boolean chooseLeft) {
            if (!placementAnswerPending) {
                return;
            }
            Node parent = placementCurrentNode;
            boolean belongsLeft = placementValue < parent.value;
            if (chooseLeft != belongsLeft) {
                placementTree.flashNode(parent, FEEDBACK_ERROR);
                placementHearts--;
                updatePlacementHearts();
                if (placementHearts <= 0) {
                    placementAnswerPending = false;
                    placementAdvanceTimer.stop();
                    String[] options = {"Restart Game", "Back to More Games"};
                    int choice = showOutcomeDialog("Game Over", "The trail got tricky",
                            "You used all three hearts. Restart this game or return to More Games.",
                            false, options);
                    if (choice == 0) {
                        startPlacementGame();
                    } else {
                        cards.show(screens, "moreGames");
                    }
                } else {
                    setFeedback(placementStatusLabel, "Incorrect. " + placementHearts
                            + " heart(s) left. Try again.", FEEDBACK_ERROR);
                }
                return;
            }

            Node nextNode = chooseLeft ? parent.left : parent.right;
            if (nextNode != null) {
                placementCurrentNode = nextNode;
                placementParentValue = nextNode.value;
                placementQuestionLabel.setText("Level " + placementLevel + "  |  Round "
                        + placementRound + " / 10: Where does "
                        + placementValue + " go compared with " + placementParentValue + "?");
                setFeedback(placementStatusLabel, "Correct. Keep following the same value down the tree.", SUCCESS);
                placementTree.setTree(placementRoot, nextNode);
                placementTree.flashNode(nextNode, SUCCESS);
                return;
            }

            placementAnswerPending = false;
            placementLeftButton.setEnabled(false);
            placementRightButton.setEnabled(false);
            if (chooseLeft) {
                parent.left = new Node(placementValue);
            } else {
                parent.right = new Node(placementValue);
            }
            placementTree.setTree(placementRoot, findNode(placementRoot, placementValue));
            placementTree.flashNode(findNode(placementRoot, placementValue), SUCCESS);
            totalScore += 2;
            updateScore();
            if (placementRound == 10) {
                placementProgress.setValue(10);
                placementProgress.setString("Level " + placementLevel + " · 10 / 10");
                setFeedback(placementStatusLabel, "Level " + placementLevel
                        + " complete! Your tree is ready for the next level.", SUCCESS);
                placementNextLevelButton.setVisible(true);
                showOutcomeDialog("Level Complete", "Branch by branch!",
                        "You built the whole tree for this level. Your next level is ready when you are.",
                        true, "Great!");
            } else {
                setFeedback(placementStatusLabel, "Correct! +2 points. Moving to the next value...", SUCCESS);
                placementRound++;
                placementProgress.setValue(placementRound - 1);
                placementProgress.setString("Level " + placementLevel + " · " + (placementRound - 1) + " / 10");
                placementAdvanceTimer.restart();
            }
        }

        private void startPlacementNextLevel() {
            if (placementRound != 10) {
                return;
            }
            placementLevel++;
            placementRound = 1;
            placementNextLevelButton.setVisible(false);
            beginPlacementQuestion();
        }

        private void startDrawTraceGame() {
            drawTraceRoot = createRandomTree(7, random);
            drawTraceNodes = traversalNodes(drawTraceRoot, selectedDrawTraceOrder());
            drawnTraceNodes.clear();
            drawTraceActive = false;
            setDrawTraceOrderEnabled(true);
            drawTraceTimer.stop();
            drawTraceTimerLabel.setSeconds(15);
            drawTraceTree.setTracePath(drawTraceRoot, drawnTraceNodes, null);
            setFeedback(drawTraceStatus, "Choose a traversal and click Start Trace.", MUTED);
            updateDrawTraceRule();
            drawTraceOutput.setText("Path: (none)");
            drawTraceProgress.setMaximum(drawTraceNodes.size());
            drawTraceProgress.setValue(0);
            drawTraceProgress.setString("0 / " + drawTraceNodes.size() + " nodes");
            cards.show(screens, "drawTrace");
        }

        private void resetDrawTrace() {
            if (drawTraceRoot == null) {
                drawTraceRoot = createRandomTree(7, random);
            }
            drawTraceTimer.stop();
            drawTraceNodes = traversalNodes(drawTraceRoot, selectedDrawTraceOrder());
            drawnTraceNodes.clear();
            drawTraceActive = true;
            setDrawTraceOrderEnabled(false);
            drawTraceSecondsRemaining = 15;
            updateDrawTraceTimerLabel();
            drawTraceTimer.start();
            drawTraceTree.setTracePath(drawTraceRoot, drawnTraceNodes, null);
            setFeedback(drawTraceStatus, "Trace in " + selectedDrawTraceOrder().label
                    + " order before the timer reaches zero.", MUTED);
            drawTraceOutput.setText("Path: (none)");
            drawTraceProgress.setMaximum(drawTraceNodes.size());
            drawTraceProgress.setValue(0);
            drawTraceProgress.setString("0 / " + drawTraceNodes.size() + " nodes");
        }

        private void handleDrawTraceClick(Node node) {
            if (!drawTraceActive || drawnTraceNodes.size() >= drawTraceNodes.size()) {
                return;
            }
            Node expected = drawTraceNodes.get(drawnTraceNodes.size());
            if (node != expected) {
                drawTraceTree.flashNode(node, FEEDBACK_ERROR);
                setFeedback(drawTraceStatus, "Not that node yet. Follow the selected traversal.", FEEDBACK_ERROR);
                return;
            }
            drawnTraceNodes.add(node);
            drawTraceTree.setTracePath(drawTraceRoot, drawnTraceNodes, node);
            drawTraceTree.flashNode(node, SUCCESS);
            drawTraceProgress.setValue(drawnTraceNodes.size());
            drawTraceProgress.setString(drawnTraceNodes.size() + " / " + drawTraceNodes.size() + " nodes");
            List<Integer> pathValues = new ArrayList<>();
            for (Node tracedNode : drawnTraceNodes) {
                pathValues.add(tracedNode.value);
            }
            drawTraceOutput.setText("Path: " + pathValues);
            if (drawnTraceNodes.size() == drawTraceNodes.size()) {
                drawTraceActive = false;
                drawTraceTimer.stop();
                setDrawTraceOrderEnabled(true);
                setFeedback(drawTraceStatus, "Excellent! You traced the complete "
                        + selectedDrawTraceOrder().label + " path.", SUCCESS);
                totalScore += 10;
                updateScore();
                showOutcomeDialog("Trace Complete", "Excellent tracing!",
                        "You followed the full " + selectedDrawTraceOrder().label
                                + " path and earned 10 points.",
                        true, "Continue");
            } else {
                drawTraceSecondsRemaining = 15;
                updateDrawTraceTimerLabel();
                drawTraceTimer.restart();
                setFeedback(drawTraceStatus, "Correct. Continue to the next node.", SUCCESS);
            }
        }

        private TraversalOrder selectedDrawTraceOrder() {
            if (tracePreorderChoice.isSelected()) {
                return TraversalOrder.PREORDER;
            }
            if (tracePostorderChoice.isSelected()) {
                return TraversalOrder.POSTORDER;
            }
            return TraversalOrder.INORDER;
        }

        private void updateDrawTraceRule() {
            TraversalOrder order = selectedDrawTraceOrder();
            drawTraceRuleLabel.setText(order.label + ": " + order.rule);
            drawTraceNodes = drawTraceRoot == null
                    ? Collections.<Node>emptyList() : traversalNodes(drawTraceRoot, order);
            drawTraceTree.setTracePath(drawTraceRoot, Collections.<Node>emptyList(), null);
            drawTraceOutput.setText("Path: (none)");
            setFeedback(drawTraceStatus, order.label + ": " + order.rule + ". Click Start Trace when ready.", MUTED);
        }

        private void setDrawTraceOrderEnabled(boolean enabled) {
            traceInorderChoice.setEnabled(enabled);
            tracePreorderChoice.setEnabled(enabled);
            tracePostorderChoice.setEnabled(enabled);
        }

        private void tickDrawTraceTimer() {
            if (!drawTraceActive) {
                drawTraceTimer.stop();
                return;
            }
            drawTraceSecondsRemaining--;
            updateDrawTraceTimerLabel();
            if (drawTraceSecondsRemaining <= 0) {
                drawTraceActive = false;
                drawTraceTimer.stop();
                setDrawTraceOrderEnabled(true);
                setFeedback(drawTraceStatus, "Time's up! Click Restart Trace to try again.", FEEDBACK_ERROR);
            }
        }

        private void updateDrawTraceTimerLabel() {
            drawTraceTimerLabel.setSeconds(drawTraceSecondsRemaining);
            drawTraceTimerLabel.setForeground(drawTraceSecondsRemaining <= 3
                    ? new Color(153, 73, 54) : ACCENT);
            if (Boolean.TRUE.equals(drawTraceTimerLabel.getClientProperty("statusBadge"))) {
                boolean urgent = drawTraceSecondsRemaining <= 3;
                drawTraceTimerLabel.setBackground(urgent
                        ? new Color(250, 239, 235) : statusBackground(ACCENT));
                drawTraceTimerLabel.setBorder(new RoundedBorder(urgent
                        ? new Color(225, 190, 179) : statusBorder(ACCENT), 14, 6, 12));
            }
        }

        private void updatePlacementHearts() {
            placementHeartsLabel.setLives(placementHearts);
        }

        private void beginTraversalLevel() {
            int nodeCount = Math.min(6 + traversalLevel, 20);
            traversalRoot = createRandomTree(nodeCount, random);
            traversalLevelLabel.setText("Level " + traversalLevel + "  ·  " + nodeCount + " nodes");
            traversalRule.setText(selectedTraversalOrder.label + ": " + selectedTraversalOrder.rule);
            prepareTraversalLevel();
        }

        private void prepareTraversalLevel() {
            stopTraversalTimer();
            traceNodes = traversalNodes(traversalRoot, selectedTraversalOrder);
            tracedValues.clear();
            orderedNodes.clear();
            orderingActive = false;
            traversalHeartsRemaining = 3;
            traversalSecondsRemaining = 10;
            traceOutput.setText("Your order: (none)");
            setFeedback(traceStatus, "Click Start Ordering when you're ready. You have 10 seconds per value.", MUTED);
            updateTraversalHearts();
            updateTraversalTimerLabel();
            traversalValueInput.setText("");
            traversalValueInput.setEnabled(false);
            traversalTree.setTraversalProgress(traversalRoot, null, orderedNodes);
            traversalProgress.setMaximum(traceNodes.size());
            traversalProgress.setValue(0);
            traversalProgress.setString("0 / " + traceNodes.size() + " values");
        }

        private void startTrace() {
            traceNodes = traversalNodes(traversalRoot, selectedTraversalOrder);
            tracedValues.clear();
            orderedNodes.clear();
            traversalHeartsRemaining = 3;
            orderingActive = !traceNodes.isEmpty();
            traceOutput.setText("Your order: (none)");
            setFeedback(traceStatus, "Enter the values in " + selectedTraversalOrder.label
                    + " order (" + traceNodes.size() + " values).", MUTED);
            updateTraversalHearts();
            traversalValueInput.setText("");
            traversalValueInput.setEnabled(orderingActive);
            if (orderingActive) {
                startTraversalTimer();
            } else {
                stopTraversalTimer();
            }
            traversalValueInput.requestFocusInWindow();
            traversalTree.setTraversalProgress(traversalRoot, null, orderedNodes);
            traversalProgress.setMaximum(traceNodes.size());
            traversalProgress.setValue(0);
            traversalProgress.setString("0 / " + traceNodes.size() + " values");
        }

        private void resetTraversalRound() {
            beginTraversalLevel();
            setFeedback(traceStatus, "Round reset. Click Start Ordering to begin again.", MUTED);
            traversalValueInput.setEnabled(false);
            traversalValueInput.setText("");
            traceOutput.setText("Your order: (none)");
            stopTraversalTimer();
        }

        private void submitTraversalValue() {
            if (!orderingActive) {
                setFeedback(traceStatus, "Choose an order and click Start Ordering first.", WARNING);
                return;
            }
            if (tracedValues.size() >= traceNodes.size()) {
                return;
            }

            Node expectedNode = traceNodes.get(tracedValues.size());
            int enteredValue;
            try {
                enteredValue = Integer.parseInt(traversalValueInput.getText().trim());
            } catch (NumberFormatException exception) {
                traversalTree.flashNode(expectedNode, FEEDBACK_ERROR);
                loseTraversalHeart();
                return;
            }
            if (enteredValue != expectedNode.value) {
                traversalTree.flashNode(expectedNode, FEEDBACK_ERROR);
                loseTraversalHeart();
                traversalValueInput.selectAll();
                if (orderingActive) {
                    traversalValueInput.requestFocusInWindow();
                }
                return;
            }

            stopTraversalTimer();
            orderedNodes.add(expectedNode);
            tracedValues.add(enteredValue);
            traversalProgress.setValue(tracedValues.size());
            traversalProgress.setString(tracedValues.size() + " / " + traceNodes.size() + " values");
            traceOutput.setText("Your order: " + tracedValues);
            traversalValueInput.setText("");
            traversalTree.setTraversalProgress(traversalRoot, expectedNode, orderedNodes);

            if (tracedValues.size() == traceNodes.size()) {
                orderingActive = false;
                traversalValueInput.setEnabled(false);
                traversalTimerLabel.setComplete();
                setFeedback(traceStatus, "Correct! You completed the " + selectedTraversalOrder.label
                        + " traversal.", SUCCESS);
                offerNextTraversalLevel();
            } else {
                setFeedback(traceStatus, "Correct! " + tracedValues.size() + " of "
                        + traceNodes.size() + " values correct. Enter the next value.", SUCCESS);
                startTraversalTimer();
                traversalValueInput.requestFocusInWindow();
            }
        }

        private void styleTraversalChoice(JButton choice, TraversalOrder order) {
            boolean selected = selectedTraversalOrder == order;
            boolean focused = choice.isFocusOwner();
            choice.setText((selected ? "\u2713  " : "") + order.label);
            choice.setBackground(selected ? new Color(246, 224, 158)
                    : choice.getModel().isRollover() ? BUTTON_HOVER : new Color(255, 252, 241));
            choice.setForeground(selected ? TEXT : ACCENT);
            choice.setToolTipText(selected ? order.label + " is selected" : "Select " + order.label);
            choice.getAccessibleContext().setAccessibleDescription(
                    selected ? "Selected traversal order" : "Select this traversal order");
            choice.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(focused ? new Color(231, 185, 77)
                                    : selected ? new Color(174, 128, 35) : BORDER, focused ? 3 : 1),
                            BorderFactory.createEmptyBorder(selected ? 1 : 3,
                                    selected ? 1 : 3, selected ? 1 : 3, selected ? 1 : 3)),
                    BorderFactory.createEmptyBorder(7, 12, 7, 12)));
            choice.setFocusPainted(false);
        }

        private void prepareTraversalChoice(JButton choice, TraversalOrder order) {
            choice.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
            choice.setOpaque(true);
            choice.setContentAreaFilled(true);
            choice.setRolloverEnabled(true);
            choice.setFocusPainted(false);
            choice.getAccessibleContext().setAccessibleName(order.label + " traversal");
            choice.addChangeListener(event -> styleTraversalChoice(choice, order));
            choice.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    styleTraversalChoice(choice, order);
                }

                @Override
                public void focusLost(FocusEvent event) {
                    styleTraversalChoice(choice, order);
                }
            });
        }

        private void styleSelectionChoice(JRadioButton choice) {
            choice.setFont(BODY_FONT.deriveFont(Font.BOLD, 12f));
            choice.setForeground(TEXT);
            choice.setBackground(new Color(255, 252, 241));
            choice.setOpaque(true);
            choice.setFocusPainted(false);
            choice.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            choice.getAccessibleContext().setAccessibleDescription("Traversal order option");
            choice.addChangeListener(event -> refreshSelectionChoice(choice));
            choice.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    refreshSelectionChoice(choice);
                }

                @Override
                public void focusLost(FocusEvent event) {
                    refreshSelectionChoice(choice);
                }
            });
        }

        private void refreshSelectionChoice(JRadioButton choice) {
            boolean selected = choice.isSelected();
            boolean focused = choice.isFocusOwner();
            choice.setBackground(selected ? new Color(246, 224, 158)
                    : choice.getModel().isRollover() ? new Color(237, 243, 229)
                    : new Color(255, 252, 241));
            choice.setFont(BODY_FONT.deriveFont(Font.BOLD, selected ? 12.5f : 12f));
            choice.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(focused ? new Color(231, 185, 77)
                            : selected ? new Color(174, 128, 35) : BORDER, focused ? 3 : selected ? 2 : 1),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            choice.getAccessibleContext().setAccessibleDescription(
                    selected ? "Selected traversal order" : "Select this traversal order");
        }

        private void clearTrace() {
            stopTraversalTimer();
            traceNodes = Collections.emptyList();
            tracedValues.clear();
            orderedNodes.clear();
            orderingActive = false;
            setFeedback(traceStatus, "Choose an order, start, then enter values in sequence.", MUTED);
            traceOutput.setText("Your order: (none)");
            traversalValueInput.setText("");
            traversalValueInput.setEnabled(false);
            traversalSecondsRemaining = 10;
            updateTraversalTimerLabel();
            traversalRule.setText(selectedTraversalOrder.label + ": " + selectedTraversalOrder.rule);
            traversalTree.setTraversalProgress(traversalRoot, null, orderedNodes);
        }

        private void offerNextTraversalLevel() {
            stopTraversalTimer();
            int choice = showOutcomeDialog("Level Complete", "Brilliant traversal!",
                    "You completed Level " + traversalLevel + ". Ready for a new tree and the next level?",
                    true, "Next Level", "Back to Menu");
            if (choice == 0) {
                traversalLevel++;
                beginTraversalLevel();
            } else {
                showHome();
            }
        }

        private void loseTraversalHeart() {
            traversalHeartsRemaining--;
            updateTraversalHearts();
            if (traversalHeartsRemaining == 0) {
                stopTraversalTimer();
                orderingActive = false;
                traversalValueInput.setEnabled(false);
                traversalTimerLabel.setSeconds(0);
                setFeedback(traceStatus, "No hearts left.", FEEDBACK_ERROR);
                String[] options = {"Restart Game", "Back to Menu"};
                int choice = showOutcomeDialog("Game Over", "Keep exploring!",
                        "You used all three hearts. Restart the game or head back to the menu.",
                        false, options);
                if (choice == 0) {
                    beginSelectedTraversalGame();
                } else {
                    showHome();
                }
            } else {
                setFeedback(traceStatus, "Incorrect value. " + traversalHeartsRemaining
                        + " heart(s) remaining. Try again.", FEEDBACK_ERROR);
            }
        }

        private void startTraversalTimer() {
            traversalTimer.stop();
            traversalSecondsRemaining = 10;
            updateTraversalTimerLabel();
            traversalTimer.start();
        }

        private void stopTraversalTimer() {
            traversalTimer.stop();
        }

        private void tickTraversalTimer() {
            if (!orderingActive) {
                stopTraversalTimer();
                return;
            }
            traversalSecondsRemaining--;
            updateTraversalTimerLabel();
            if (traversalSecondsRemaining <= 0) {
                stopTraversalTimer();
                loseTraversalHeart();
                if (orderingActive && traversalHeartsRemaining > 0) {
                    setFeedback(traceStatus, "Time ran out! " + traversalHeartsRemaining
                            + " heart(s) remaining. Enter the value again.", FEEDBACK_ERROR);
                    startTraversalTimer();
                }
            }
        }

        private void updateTraversalTimerLabel() {
            boolean urgent = traversalSecondsRemaining <= 3;
            traversalTimerLabel.setSeconds(traversalSecondsRemaining);
            traversalTimerLabel.setForeground(urgent ? new Color(153, 73, 54) : ACCENT);
            if (Boolean.TRUE.equals(traversalTimerLabel.getClientProperty("statusBadge"))) {
                traversalTimerLabel.setBackground(urgent
                        ? new Color(250, 239, 235) : statusBackground(ACCENT));
                traversalTimerLabel.setBorder(new RoundedBorder(urgent
                        ? new Color(225, 190, 179) : statusBorder(ACCENT), 14, 6, 12));
            }
        }

        private void updateTraversalHearts() {
            traversalHearts.setLives(traversalHeartsRemaining);
        }

        private void practiceAction(boolean inserting) {
            int value;
            try {
                value = Integer.parseInt(practiceInput.getText().trim());
            } catch (NumberFormatException exception) {
                setFeedback(practiceMessage, "Enter a whole number from 1 to 999.", FEEDBACK_ERROR);
                return;
            }
            if (value < 1 || value > 999) {
                setFeedback(practiceMessage, "Enter a whole number from 1 to 999.", FEEDBACK_ERROR);
                return;
            }

            if (inserting) {
                if (search(practiceRoot, value)) {
                    setFeedback(practiceMessage, value + " is already in the tree.", WARNING);
                } else {
                    practiceRoot = insert(practiceRoot, value);
                    totalScore += 2;
                    setFeedback(practiceMessage, value + " inserted! +2 practice points.", SUCCESS);
                    clearTrace();
                    refreshPracticeTree(null);
                    updateScore();
                }
            } else {
                clearTrace();
                Node found = findNode(practiceRoot, value);
                if (found == null) {
                    setFeedback(practiceMessage, value + " is not in this tree.", WARNING);
                    refreshPracticeTree(null);
                } else if (PRACTICE_SEARCH_REWARDS.recordSuccessfulSearch(value)) {
                    totalScore += 3;
                    setFeedback(practiceMessage, "Found " + value + "! +3 points.", SUCCESS);
                    refreshPracticeTree(found);
                    updateScore();
                } else {
                    setFeedback(practiceMessage, "Found " + value + "! Points for this value were already earned.",
                            WARNING);
                    refreshPracticeTree(found);
                }
            }
        }

        private Node findNode(Node root, int value) {
            Node current = root;
            while (current != null) {
                if (current.value == value) {
                    return current;
                }
                current = value < current.value ? current.left : current.right;
            }
            return null;
        }
    }

    private static class ScrollablePage extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        private ScreenScene scene = ScreenScene.PRACTICE;

        void setScene(ScreenScene scene) {
            this.scene = scene;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Color top = sceneTopColor();
            Color bottom = sceneBottomColor();
            g.setPaint(new java.awt.GradientPaint(0, 0, top, width, height, bottom));
            g.fillRect(0, 0, width, height);
            if (scene == ScreenScene.MENU) {
                drawLeafSprig(g, 18, 118, 1.0, 28);
                drawLeafSprig(g, width - 18, Math.max(160, height - 100), -1.0, 28);
            } else {
                Color decoration = scene == ScreenScene.CAMPAIGN ? new Color(88, 126, 81, 35)
                        : scene == ScreenScene.TRAVERSAL ? new Color(107, 126, 99, 27)
                        : scene == ScreenScene.MINI_GAME ? new Color(115, 137, 101, 30)
                        : new Color(105, 132, 91, 35);
                g.setColor(decoration);
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine(12, 14, width - 12, 14);
                g.drawLine(12, Math.max(14, height - 14), width - 12, Math.max(14, height - 14));
                int accent = scene == ScreenScene.CAMPAIGN ? 22 : 12;
                drawLeafSprig(g, 8, 54, 1.0, accent);
                drawLeafSprig(g, width - 8, Math.max(80, height - 48), -1.0, accent);
                drawSceneLandmarks(g, width, height);
            }
            g.dispose();
        }

        private Color sceneTopColor() {
            return switch (scene) {
                case MENU -> new Color(249, 247, 232);
                case CAMPAIGN -> new Color(240, 244, 225);
                case TRAVERSAL -> new Color(241, 244, 234);
                case MINI_GAME -> new Color(247, 243, 225);
                default -> new Color(247, 246, 233);
            };
        }

        private Color sceneBottomColor() {
            return switch (scene) {
                case MENU -> new Color(235, 239, 222);
                case CAMPAIGN -> new Color(225, 236, 216);
                case TRAVERSAL -> new Color(229, 237, 224);
                case MINI_GAME -> new Color(239, 232, 210);
                default -> new Color(239, 241, 227);
            };
        }

        private void drawSceneLandmarks(Graphics2D g, int width, int height) {
            g.setColor(new Color(96, 128, 82, 18));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (scene) {
                case CAMPAIGN -> {
                    g.drawOval(18, 28, 26, 26);
                    g.drawOval(width - 44, Math.max(34, height - 74), 24, 24);
                    g.drawLine(32, 54, width - 32, height - 52);
                }
                case TRAVERSAL -> {
                    g.drawLine(22, 46, 38, 32);
                    g.drawLine(38, 32, 52, 46);
                    g.drawLine(width - 52, Math.max(48, height - 50),
                            width - 38, Math.max(34, height - 64));
                    g.drawLine(width - 38, Math.max(34, height - 64),
                            width - 22, Math.max(48, height - 50));
                }
                case MINI_GAME -> {
                    for (int i = 0; i < 3; i++) {
                        g.drawOval(20 + i * 10, 28 + i * 10, 7, 7);
                        g.drawOval(width - 48 + i * 8, Math.max(34, height - 60 + i * 8), 6, 6);
                    }
                }
                default -> {
                    drawLeafSprig(g, 8, 54, 1.0, 14);
                    drawLeafSprig(g, width - 8, Math.max(80, height - 48), -1.0, 14);
                }
            }
        }

        private void drawLeafSprig(Graphics2D g, int x, int y, double direction, int alpha) {
            g.setColor(new Color(93, 128, 84, alpha));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x, y + 56, x + (int) (direction * 28), y);
            for (int i = 0; i < 4; i++) {
                int leafX = x + (int) (direction * i * 7);
                int leafY = y + 43 - i * 13;
                g.fillOval(leafX - 12, leafY - 8, 14, 8);
                g.fillOval(leafX + 1, leafY - 2, 14, 8);
            }
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getPreferredSize().height <= getParent().getHeight();
        }
    }

    private static class MiniGameIcon extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int style;

        MiniGameIcon(String title) {
            style = title.startsWith("Arrange") ? 0 : title.startsWith("Trace") ? 1 : 2;
            setOpaque(false);
            setPreferredSize(new Dimension(54, 54));
            setMinimumSize(new Dimension(54, 54));
            setMaximumSize(new Dimension(54, 54));
            getAccessibleContext().setAccessibleName(title + " icon");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(226, 235, 213));
            g.fillOval(2, 2, 50, 50);
            g.setColor(new Color(192, 207, 174));
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval(2, 2, 50, 50);
            switch (style) {
                case 0 -> {
                    g.setColor(ACCENT);
                    g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(17, 19, 27, 28);
                    g.drawLine(37, 19, 27, 28);
                    drawNode(g, 27, 15, "1", new Color(242, 193, 78));
                    drawNode(g, 15, 38, "2", new Color(63, 123, 83));
                    drawNode(g, 39, 38, "3", new Color(63, 123, 83));
                }
                case 1 -> {
                    g.setColor(ACCENT);
                    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(16, 18, 36, 18);
                    g.drawLine(36, 18, 36, 36);
                    g.drawLine(36, 36, 20, 36);
                    g.fillOval(12, 14, 8, 8);
                    g.fillOval(32, 14, 8, 8);
                    g.fillOval(32, 32, 8, 8);
                    g.setColor(new Color(230, 180, 66));
                    g.fillOval(16, 32, 8, 8);
                }
                default -> {
                    g.setColor(new Color(126, 165, 116));
                    g.fillOval(14, 15, 10, 10);
                    g.fillOval(31, 15, 10, 10);
                    g.setColor(TEXT);
                    g.setFont(BODY_FONT.deriveFont(Font.BOLD, 17f));
                    g.drawString("?", 20, 39);
                }
            }
            g.dispose();
        }

        private void drawNode(Graphics2D g, int x, int y, String value, Color color) {
            g.setColor(color);
            g.fillOval(x - 8, y - 8, 16, 16);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval(x - 8, y - 8, 16, 16);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 8f));
            int width = g.getFontMetrics().stringWidth(value);
            g.drawString(value, x - width / 2, y + 3);
        }
    }

    private static class ScoreBadge extends JPanel {
        private static final long serialVersionUID = 1L;
        private int score;

        ScoreBadge() {
            setOpaque(false);
            setPreferredSize(new Dimension(140, 46));
            setMinimumSize(new Dimension(130, 42));
            setMaximumSize(new Dimension(160, 50));
            setAlignmentX(CENTER_ALIGNMENT);
            getAccessibleContext().setAccessibleName("Total score");
            getAccessibleContext().setAccessibleDescription("Displays the player's current points.");
        }

        void setScore(int score) {
            this.score = score;
            getAccessibleContext().setAccessibleName("Total score: " + score + " points");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(46, 57, 39, 28));
            g.fillRoundRect(3, 5, width - 6, height - 8, 22, 22);
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(255, 254, 245),
                    0, height, new Color(245, 239, 213)));
            g.fillRoundRect(2, 2, width - 6, height - 9, 22, 22);
            g.setColor(new Color(203, 177, 112));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(2, 2, width - 6, height - 9, 22, 22);
            g.setColor(new Color(255, 255, 250, 210));
            g.drawRoundRect(5, 5, width - 12, height - 15, 19, 19);

            int centerX = 22;
            int centerY = height / 2 - 1;
            g.setColor(new Color(227, 190, 102));
            g.fillOval(centerX - 12, centerY - 12, 24, 24);
            g.setColor(new Color(177, 135, 54));
            g.setStroke(new BasicStroke(1.1f));
            g.drawOval(centerX - 12, centerY - 12, 24, 24);
            g.setColor(new Color(255, 235, 173));
            g.fillOval(centerX - 8, centerY - 8, 16, 16);

            int[] starX = new int[10];
            int[] starY = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                int radius = i % 2 == 0 ? 7 : 3;
                starX[i] = centerX + (int) (Math.cos(angle) * radius);
                starY[i] = centerY + (int) (Math.sin(angle) * radius);
            }
            g.setColor(new Color(183, 132, 41));
            g.fillPolygon(starX, starY, starX.length);

            int textX = 42;
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 7.5f));
            g.setColor(MUTED);
            g.drawString("TOTAL", textX, 17);
            g.drawString("SCORE", textX, 28);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
            g.setColor(TEXT);
            String value = Integer.toString(score);
            g.drawString(value, textX, 41);
            g.dispose();
        }
    }

    private static class MenuIcon implements javax.swing.Icon {
        enum Kind {
            PLAY, SETTINGS, GAMES, CHECK, SEARCH, TRAIL, TREASURE
        }

        private final Kind kind;

        MenuIcon(Kind kind) {
            this.kind = kind;
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(component instanceof JButton && ((JButton) component).getForeground() != null
                    ? ((JButton) component).getForeground() : ACCENT);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case PLAY -> g.fillPolygon(new int[] {5, 16, 5}, new int[] {3, 10, 17}, 3);
                case SETTINGS -> {
                    g.drawOval(5, 5, 10, 10);
                    g.fillOval(8, 8, 4, 4);
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        int x1 = 10 + (int) (Math.cos(angle) * 6);
                        int y1 = 10 + (int) (Math.sin(angle) * 6);
                        int x2 = 10 + (int) (Math.cos(angle) * 9);
                        int y2 = 10 + (int) (Math.sin(angle) * 9);
                        g.drawLine(x1, y1, x2, y2);
                    }
                }
                case GAMES -> {
                    g.drawRoundRect(2, 3, 7, 7, 2, 2);
                    g.drawRoundRect(11, 3, 7, 7, 2, 2);
                    g.drawRoundRect(2, 12, 7, 7, 2, 2);
                    g.drawRoundRect(11, 12, 7, 7, 2, 2);
                }
                case CHECK -> {
                    g.drawLine(3, 10, 8, 15);
                    g.drawLine(8, 15, 18, 4);
                }
                case SEARCH -> {
                    g.drawOval(2, 2, 11, 11);
                    g.drawLine(11, 11, 18, 18);
                }
                case TRAIL -> {
                    g.drawLine(3, 16, 9, 10);
                    g.drawLine(9, 10, 15, 13);
                    g.drawLine(15, 13, 17, 4);
                    g.fillOval(1, 14, 4, 4);
                    g.fillOval(7, 8, 4, 4);
                    g.fillOval(13, 11, 4, 4);
                    g.fillOval(15, 2, 4, 4);
                }
                case TREASURE -> {
                    g.drawRoundRect(3, 8, 14, 9, 2, 2);
                    g.drawArc(4, 3, 12, 10, 0, 180);
                    g.drawLine(10, 9, 10, 15);
                }
                default -> {
                    // no-op
                }
            }
            g.dispose();
        }
    }

    private static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color;
        private final int arc;
        private final int verticalPadding;
        private final int horizontalPadding;

        RoundedBorder(Color color, int arc, int verticalPadding, int horizontalPadding) {
            this.color = color;
            this.arc = arc;
            this.verticalPadding = verticalPadding;
            this.horizontalPadding = horizontalPadding;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            g.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(Component component) {
            return new java.awt.Insets(verticalPadding, horizontalPadding,
                    verticalPadding, horizontalPadding);
        }
    }

    private static class StatusBadgeLabel extends JLabel {
        private static final long serialVersionUID = 1L;

        StatusBadgeLabel() {
        }

        StatusBadgeLabel(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (isOpaque()) {
                super.paintComponent(graphics);
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class LifeBadgeLabel extends StatusBadgeLabel {
        private static final long serialVersionUID = 1L;
        private int lives = 3;

        LifeBadgeLabel() {
            super("");
            setPreferredSize(new Dimension(176, 38));
            setMaximumSize(new Dimension(220, 42));
            getAccessibleContext().setAccessibleName("Lives");
        }

        void setLives(int lives) {
            this.lives = Math.max(0, Math.min(3, lives));
            getAccessibleContext().setAccessibleName("Lives remaining: " + this.lives + " of 3");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 12f));
            String label = "LIVES";
            int iconSize = 15;
            int gap = 7;
            int totalWidth = g.getFontMetrics().stringWidth(label) + 11 + iconSize * 3 + gap * 2;
            int x = (getWidth() - totalWidth) / 2;
            int baseline = getHeight() / 2 + 4;
            g.setColor(getForeground());
            g.drawString(label, x, baseline);
            x += g.getFontMetrics().stringWidth(label) + 11;
            for (int i = 0; i < 3; i++) {
                g.setColor(i < lives ? new Color(176, 76, 61) : new Color(190, 188, 174));
                drawHeart(g, x, getHeight() / 2 - 7, iconSize);
                x += iconSize + gap;
            }
            g.dispose();
        }

        private void drawHeart(Graphics2D g, int x, int y, int size) {
            Path2D.Double heart = new Path2D.Double();
            heart.moveTo(x + size / 2.0, y + size * 0.92);
            heart.curveTo(x + size * 0.35, y + size * 0.78, x, y + size * 0.50,
                    x + size * 0.08, y + size * 0.28);
            heart.curveTo(x + size * 0.14, y + size * 0.02, x + size * 0.43, y + size * 0.06,
                    x + size / 2.0, y + size * 0.28);
            heart.curveTo(x + size * 0.62, y + size * 0.04, x + size * 0.92, y + size * 0.02,
                    x + size * 0.98, y + size * 0.28);
            heart.curveTo(x + size, y + size * 0.52, x + size * 0.66, y + size * 0.80,
                    x + size / 2.0, y + size * 0.92);
            heart.closePath();
            g.fill(heart);
        }
    }

    private static class ClockBadgeLabel extends StatusBadgeLabel {
        private static final long serialVersionUID = 1L;
        private int seconds;
        private boolean complete;

        ClockBadgeLabel(int seconds) {
            super("");
            setPreferredSize(new Dimension(150, 38));
            setMaximumSize(new Dimension(190, 42));
            this.seconds = Math.max(0, seconds);
            complete = false;
            getAccessibleContext().setAccessibleName("Time remaining: " + this.seconds + " seconds");
        }

        void setSeconds(int seconds) {
            this.seconds = Math.max(0, seconds);
            complete = false;
            getAccessibleContext().setAccessibleName("Time remaining: " + this.seconds + " seconds");
            repaint();
        }

        void setComplete() {
            complete = true;
            getAccessibleContext().setAccessibleName("Timer complete");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int centerY = getHeight() / 2;
            int iconX = Math.max(12, getWidth() / 2 - 45);
            g.setColor(getForeground());
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(iconX - 8, centerY - 8, 16, 16);
            g.drawLine(iconX, centerY, iconX, centerY - 5);
            g.drawLine(iconX, centerY, iconX + 4, centerY + 2);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 13f));
            String value = complete ? "Complete" : seconds + " sec";
            g.drawString(value, iconX + 15, centerY + 5);
            g.dispose();
        }
    }

    private static class RoundedCardPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        RoundedCardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(36, 52, 39, 18));
            g.fillRoundRect(2, 3, width - 5, height - 5, 18, 18);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, width - 4, height - 5, 18, 18);
            g.setColor(BORDER);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(0, 0, width - 4, height - 5, 18, 18);
            g.setColor(ACCENT);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(16, 2, Math.max(16, width - 20), 2);
            g.dispose();
        }
    }

    private static class HeaderDivider extends JPanel {
        private static final long serialVersionUID = 1L;

        HeaderDivider() {
            setOpaque(false);
            setPreferredSize(new Dimension(220, 12));
            setMaximumSize(new Dimension(220, 12));
            setAlignmentX(CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int midY = getHeight() / 2;
            int centerX = getWidth() / 2;
            g.setColor(BORDER);
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(0, midY, centerX - 13, midY);
            g.drawLine(centerX + 13, midY, getWidth(), midY);
            g.setColor(ACCENT);
            g.fillOval(centerX - 3, midY - 3, 6, 6);
            g.fillOval(centerX - 13, midY - 4, 7, 4);
            g.fillOval(centerX + 6, midY, 7, 4);
            g.dispose();
        }
    }

    private static class CampaignTrail extends JPanel {
        private static final long serialVersionUID = 1L;
        private int completedLevels;

        CampaignTrail() {
            setOpaque(false);
            setPreferredSize(new Dimension(360, 48));
            setMaximumSize(new Dimension(520, 48));
            getAccessibleContext().setAccessibleName("Five-step campaign trail");
            getAccessibleContext().setAccessibleDescription("Shows completed campaign levels and the current level.");
        }

        void setCompletedLevels(int completedLevels) {
            this.completedLevels = Math.max(0, Math.min(5, completedLevels));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int centerY = 21;
            int left = 28;
            int right = getWidth() - 28;
            int spacing = (right - left) / 4;
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                int x1 = left + i * spacing;
                int x2 = x1 + spacing;
                g.setColor(i < completedLevels ? ACCENT : new Color(201, 204, 180));
                g.drawLine(x1, centerY, x2, centerY);
            }
            for (int i = 0; i < 5; i++) {
                int x = left + i * spacing;
                boolean completed = i < completedLevels;
                boolean current = i == completedLevels && completedLevels < 5;
                int diameter = current ? 26 : 21;
                g.setColor(current ? new Color(242, 193, 78)
                        : completed ? ACCENT : new Color(249, 247, 231));
                g.fillOval(x - diameter / 2, centerY - diameter / 2, diameter, diameter);
                g.setColor(current ? new Color(147, 100, 29)
                        : completed ? new Color(228, 241, 225) : new Color(151, 158, 130));
                g.setStroke(new BasicStroke(current ? 2.5f : 1.5f));
                g.drawOval(x - diameter / 2, centerY - diameter / 2, diameter, diameter);
                g.setFont(BODY_FONT.deriveFont(Font.BOLD, 10f));
                String number = Integer.toString(i + 1);
                int textWidth = g.getFontMetrics().stringWidth(number);
                g.setColor(current ? TEXT : completed ? Color.WHITE : MUTED);
                g.drawString(number, x - textWidth / 2, centerY + 4);
                if (completed) {
                    g.setColor(new Color(230, 180, 66));
                    g.fillOval(x - 3, centerY - 17, 6, 6);
                }
            }
            g.dispose();
        }
    }

    private static class AdventureBanner extends JPanel {
        private static final long serialVersionUID = 1L;

        AdventureBanner() {
            setOpaque(true);
            setBackground(new Color(230, 237, 216));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(189, 181, 139), 2),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            setPreferredSize(new Dimension(700, 178));
            setMinimumSize(new Dimension(340, 148));
            setMaximumSize(new Dimension(760, 178));
            getAccessibleContext().setAccessibleName(
                    "Illustrated forest path from a binary tree to a treasure chest");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(190, 218, 215),
                    0, height, new Color(248, 233, 188)));
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(255, 246, 201, 115));
            g.fillOval(width - 142, 8, 100, 100);
            g.setColor(new Color(255, 239, 176));
            g.fillOval(width - 126, 24, 68, 68);
            drawCloud(g, (int) (width * 0.66), 30, 1.0);
            drawCloud(g, (int) (width * 0.48), 55, 0.62);

            Path2D.Double farHill = new Path2D.Double();
            farHill.moveTo(0, height * 0.61);
            farHill.curveTo(width * 0.25, height * 0.36, width * 0.4, height * 0.69, width * 0.67, height * 0.46);
            farHill.curveTo(width * 0.82, height * 0.34, width * 0.91, height * 0.44, width, height * 0.35);
            farHill.lineTo(width, height);
            farHill.lineTo(0, height);
            farHill.closePath();
            g.setColor(new Color(206, 222, 193));
            g.fill(farHill);

            Path2D.Double nearHill = new Path2D.Double();
            nearHill.moveTo(0, height * 0.76);
            nearHill.curveTo(width * 0.22, height * 0.58, width * 0.41, height * 0.86, width * 0.62, height * 0.68);
            nearHill.curveTo(width * 0.78, height * 0.55, width * 0.89, height * 0.7, width, height * 0.58);
            nearHill.lineTo(width, height);
            nearHill.lineTo(0, height);
            nearHill.closePath();
            g.setColor(new Color(174, 202, 157));
            g.fill(nearHill);

            g.setColor(new Color(126, 165, 116));
            g.fillOval((int) (width * 0.52), (int) (height * 0.63), 120, 48);
            g.fillOval((int) (width * 0.72), (int) (height * 0.59), 105, 56);

            Path2D.Double path = new Path2D.Double();
            path.moveTo(width * 0.02, height * 1.03);
            path.curveTo(width * 0.18, height * 0.72, width * 0.32, height * 1.02, width * 0.44, height * 0.79);
            path.curveTo(width * 0.55, height * 0.58, width * 0.68, height * 0.83, width * 0.78, height * 0.6);
            path.curveTo(width * 0.84, height * 0.46, width * 0.9, height * 0.5, width * 0.95, height * 0.43);
            g.setColor(new Color(246, 228, 180));
            g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
            g.setColor(new Color(219, 185, 117));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);

            drawCanopyTree(g, (int) (width * 0.87), (int) (height * 0.64), 1.02);
            drawCanopyTree(g, (int) (width * 0.12), (int) (height * 0.65), 0.86);
            drawCanopyTree(g, (int) (width * 0.57), (int) (height * 0.68), 0.55);
            drawBinaryTree(g, width, height);
            drawTreasureChest(g, width, height);
            drawForegroundPlants(g, width, height);
            drawFireflies(g, width, height);
            g.dispose();
        }

        private void drawForegroundPlants(Graphics2D g, int width, int height) {
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int side = 0; side < 2; side++) {
                int x = side == 0 ? 16 : width - 16;
                int direction = side == 0 ? 1 : -1;
                int baseY = height - 5;
                g.setColor(new Color(70, 111, 73, 120));
                for (int i = 0; i < 3; i++) {
                    int stemX = x + direction * i * 8;
                    int stemHeight = 18 + i * 5;
                    g.drawLine(stemX, baseY, stemX + direction * 3, baseY - stemHeight);
                    g.setColor(new Color(96, 142, 88, 145));
                    int leafX = direction > 0 ? stemX + 2 : stemX - 11;
                    g.fillOval(leafX, baseY - stemHeight + 3, 10, 6);
                    g.setColor(new Color(70, 111, 73, 120));
                }
            }
        }

        private void drawCloud(Graphics2D g, int x, int y, double scale) {
            g.setColor(new Color(255, 255, 246, 170));
            g.fillOval(x, y + (int) (9 * scale), (int) (44 * scale), (int) (18 * scale));
            g.fillOval(x + (int) (9 * scale), y, (int) (22 * scale), (int) (25 * scale));
            g.fillOval(x + (int) (22 * scale), y + (int) (5 * scale),
                    (int) (26 * scale), (int) (19 * scale));
        }

        private void drawFireflies(Graphics2D g, int width, int height) {
            g.setColor(new Color(255, 249, 201, 220));
            int[][] points = {
                    {(int) (width * 0.51), (int) (height * 0.39)},
                    {(int) (width * 0.72), (int) (height * 0.34)},
                    {(int) (width * 0.82), (int) (height * 0.72)},
                    {(int) (width * 0.19), (int) (height * 0.43)}
            };
            for (int[] point : points) {
                g.fillOval(point[0], point[1], 5, 5);
            }
        }

        private void drawCanopyTree(Graphics2D g, int x, int y, double scale) {
            g.setColor(new Color(117, 91, 58));
            g.setStroke(new BasicStroke((float) (5 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x, y, x, y + (int) (28 * scale));
            g.drawLine(x, y + (int) (9 * scale),
                    x - (int) (11 * scale), y + (int) (1 * scale));
            g.drawLine(x, y + (int) (11 * scale),
                    x + (int) (12 * scale), y + (int) (2 * scale));
            g.setColor(new Color(73, 119, 78));
            g.fillOval(x - (int) (16 * scale), y - (int) (25 * scale),
                    (int) (32 * scale), (int) (34 * scale));
            g.setColor(new Color(99, 145, 90));
            g.fillOval(x - (int) (8 * scale), y - (int) (31 * scale),
                    (int) (21 * scale), (int) (25 * scale));
            g.setColor(new Color(132, 168, 104));
            g.fillOval(x - (int) (12 * scale), y - (int) (13 * scale),
                    (int) (12 * scale), (int) (13 * scale));
        }

        private void drawBinaryTree(Graphics2D g, int width, int height) {
            int rootX = (int) (width * 0.31);
            int rootY = (int) (height * 0.31);
            int leftX = (int) (width * 0.22);
            int rightX = (int) (width * 0.4);
            int childY = (int) (height * 0.55);
            g.setColor(new Color(79, 105, 70));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(rootX, rootY + 14, leftX, childY - 11);
            g.drawLine(rootX, rootY + 14, rightX, childY - 11);
            drawTreeNode(g, rootX, rootY, "50", new Color(229, 178, 71));
            drawTreeNode(g, leftX, childY, "30", new Color(63, 123, 83));
            drawTreeNode(g, rightX, childY, "70", new Color(63, 123, 83));
        }

        private void drawTreeNode(Graphics2D g, int x, int y, String value, Color fill) {
            g.setColor(new Color(255, 252, 241));
            g.fillOval(x - 16, y - 16, 32, 32);
            g.setColor(fill);
            g.fillOval(x - 13, y - 13, 26, 26);
            g.setColor(fill.equals(new Color(229, 178, 71)) ? TEXT : Color.WHITE);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 10f));
            int textWidth = g.getFontMetrics().stringWidth(value);
            g.drawString(value, x - textWidth / 2, y + 4);
        }

        private void drawTreasureChest(Graphics2D g, int width, int height) {
            int x = (int) (width * 0.94);
            int y = (int) (height * 0.47);
            g.setColor(new Color(133, 82, 43));
            g.fillRoundRect(x - 13, y, 27, 19, 4, 4);
            g.setColor(new Color(201, 143, 55));
            g.fillRoundRect(x - 14, y - 7, 29, 11, 5, 5);
            g.setColor(new Color(245, 211, 112));
            g.fillRect(x - 2, y - 2, 4, 8);
            g.setColor(new Color(99, 65, 39));
            g.drawRoundRect(x - 13, y, 27, 19, 4, 4);
        }
    }

    private static class OutcomeBadge extends JPanel {
        private static final long serialVersionUID = 1L;
        private final boolean success;
        private int sparkleFrame;
        private final Timer sparkleTimer = new Timer(90, event -> {
            sparkleFrame++;
            if (sparkleFrame >= 18) {
                ((Timer) event.getSource()).stop();
            }
            repaint();
        });

        OutcomeBadge(boolean success) {
            this.success = success;
            setOpaque(false);
            setPreferredSize(new Dimension(72, 72));
            setMaximumSize(new Dimension(72, 72));
            getAccessibleContext().setAccessibleName(success ? "Success badge" : "Retry badge");
            if (success && !reducedMotion) {
                sparkleTimer.start();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int center = 36;
            g.setColor(success ? new Color(220, 235, 206) : new Color(239, 222, 201));
            g.fillOval(4, 4, 64, 64);
            g.setColor(success ? ACCENT : new Color(151, 101, 69));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(4, 4, 64, 64);

            if (success) {
                int[] starX = new int[10];
                int[] starY = new int[10];
                for (int i = 0; i < 10; i++) {
                    double angle = -Math.PI / 2 + i * Math.PI / 5;
                    int radius = i % 2 == 0 ? 23 : 11;
                    starX[i] = center + (int) (Math.cos(angle) * radius);
                    starY[i] = center + (int) (Math.sin(angle) * radius);
                }
                g.setColor(new Color(230, 180, 66));
                g.fillPolygon(starX, starY, starX.length);
                g.setColor(new Color(155, 111, 36));
                g.drawPolygon(starX, starY, starX.length);
                if (!reducedMotion && sparkleFrame < 18) {
                    float fade = 1f - sparkleFrame / 18f;
                    g.setColor(new Color(230, 180, 66, Math.max(0, Math.min(255,
                            Math.round(220 * fade)))));
                    g.setStroke(new BasicStroke(1.8f));
                    for (int i = 0; i < 4; i++) {
                        double angle = i * Math.PI / 2 + sparkleFrame * 0.12;
                        int radius = 27 + sparkleFrame % 4;
                        int x = center + (int) (Math.cos(angle) * radius);
                        int y = center + (int) (Math.sin(angle) * radius);
                        g.drawLine(x - 3, y, x + 3, y);
                        g.drawLine(x, y - 3, x, y + 3);
                    }
                }
            } else {
                g.setColor(new Color(151, 101, 69));
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(20, 19, 31, 31, 40, 285);
                g.fillPolygon(new int[] {48, 57, 45}, new int[] {16, 24, 28}, 3);
                g.drawLine(24, 52, 48, 52);
            }
            g.dispose();
        }
    }

    private static class TreeView extends JPanel {
        private static final long serialVersionUID = 1L;
        private final TreePanel diagram = new TreePanel();
        private final JLabel zoomLabel = new JLabel("100%");

        TreeView() {
            super(new BorderLayout(0, 0));
            setBackground(PANEL);
            JPanel toolbar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 3));
            toolbar.setBackground(new Color(242, 241, 225));
            toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

            JLabel hint = new JLabel("Tree view  ·  scroll to zoom, drag to pan");
            hint.setFont(BODY_FONT.deriveFont(Font.PLAIN, 11f));
            hint.setForeground(MUTED);
            hint.getAccessibleContext().setAccessibleName("Tree zoom and pan instructions");
            toolbar.add(hint);

            JButton zoomOut = zoomButton("−", "Zoom out");
            zoomOut.addActionListener(event -> diagram.zoomBy(1.0 / 1.2));
            toolbar.add(zoomOut);
            JButton zoomIn = zoomButton("+", "Zoom in");
            zoomIn.addActionListener(event -> diagram.zoomBy(1.2));
            toolbar.add(zoomIn);
            zoomLabel.setFont(BODY_FONT.deriveFont(Font.BOLD, 11f));
            zoomLabel.setForeground(TEXT);
            zoomLabel.setPreferredSize(new Dimension(42, 22));
            zoomLabel.setHorizontalAlignment(JLabel.CENTER);
            zoomLabel.getAccessibleContext().setAccessibleName("Current tree zoom");
            toolbar.add(zoomLabel);
            JButton reset = zoomButton("Reset", "Reset tree zoom and position");
            reset.setPreferredSize(new Dimension(62, 26));
            reset.addActionListener(event -> diagram.resetZoom());
            toolbar.add(reset);

            diagram.setZoomChangedListener(percent -> zoomLabel.setText(percent + "%"));
            add(toolbar, BorderLayout.NORTH);
            add(diagram, BorderLayout.CENTER);
            getAccessibleContext().setAccessibleName("Interactive binary search tree diagram");
        }

        private JButton zoomButton(String text, String description) {
            JButton button = new JButton(text);
            button.setFont(BODY_FONT.deriveFont(Font.BOLD, 12f));
            button.setForeground(TEXT);
            button.setBackground(PANEL);
            button.setFocusPainted(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)));
            button.setPreferredSize(new Dimension(30, 26));
            button.setToolTipText(description);
            button.getAccessibleContext().setAccessibleDescription(description);
            return button;
        }

        void setTree(Node root, Node highlighted) {
            diagram.setTree(root, highlighted);
        }

        void setTraversalProgress(Node root, Node highlighted, Set<Node> completedNodes) {
            diagram.setTraversalProgress(root, highlighted, completedNodes);
        }

        void setTracePath(Node root, List<Node> path, Node highlighted) {
            diagram.setTracePath(root, path, highlighted);
        }

        void celebratePath(List<Node> path) {
            diagram.celebratePath(path);
        }

        void flashNode(Node node, Color color) {
            diagram.flashNode(node, color);
        }

        void setNodeClickListener(java.util.function.Consumer<Node> listener) {
            diagram.setNodeClickListener(listener);
        }

        void refreshAppearance() {
            diagram.refreshAppearance();
        }
    }

    private static class TreePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private Node root;
        private Node highlighted;
        private Set<Node> completedNodes = Collections.emptySet();
        private List<Node> tracePath = Collections.emptyList();
        private final Set<Node> knownNodes = new HashSet<>();
        private java.util.function.Consumer<Node> nodeClickListener;
        private final Map<Node, java.awt.Point> positions = new HashMap<>();
        private final Map<Integer, Integer> inorderPositions = new HashMap<>();
        private int nodeCount;
        private Node flashingNode;
        private Color flashColor;
        private float flashStrength;
        private double zoom = 1.0;
        private double panX;
        private double panY;
        private java.awt.Point dragStart;
        private double dragPanX;
        private double dragPanY;
        private boolean dragged;
        private boolean suppressNextClick;
        private Node animatingNode;
        private float insertionScale = 1f;
        private float pathAnimationPhase;
        private Node celebrationNode;
        private java.util.function.IntConsumer zoomChangedListener = percent -> {};
        private final Timer flashTimer = new Timer(45, event -> {
            flashStrength -= 0.12f;
            if (flashStrength <= 0f) {
                flashStrength = 0f;
                flashingNode = null;
                ((Timer) event.getSource()).stop();
            }
            repaint();
        });
        private final Timer insertionTimer = new Timer(32, event -> {
            insertionScale = Math.min(1f, insertionScale + 0.14f);
            if (insertionScale >= 1f) {
                animatingNode = null;
                ((Timer) event.getSource()).stop();
            }
            repaint();
        });
        private final Timer pathAnimationTimer = new Timer(60, event -> {
            pathAnimationPhase = (pathAnimationPhase + 1.3f) % 12f;
            repaint();
        });
        private static final Color UNVISITED_NODE = new Color(63, 123, 83);
        private static final Color COMPLETED_NODE = new Color(42, 126, 113);
        private static final Color ACTIVE_NODE = new Color(242, 193, 78);

        TreePanel() {
            setOpaque(true);
            setBackground(new Color(238, 235, 217));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            setToolTipText("Tree diagram. Green nodes are not visited, teal nodes are completed, and the gold node is selected.");
            MouseAdapter interaction = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    dragStart = event.getPoint();
                    dragPanX = panX;
                    dragPanY = panY;
                    dragged = false;
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    suppressNextClick = dragged;
                    dragStart = null;
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (suppressNextClick) {
                        suppressNextClick = false;
                        return;
                    }
                    if (nodeClickListener == null) {
                        return;
                    }
                    int diameter = nodeDiameter();
                    java.awt.Point logicalPoint = toLogicalPoint(event.getPoint());
                    for (Map.Entry<Node, java.awt.Point> entry : positions.entrySet()) {
                        java.awt.Point point = entry.getValue();
                        if (point.distance(logicalPoint) <= diameter / 2.0 + 4) {
                            nodeClickListener.accept(entry.getKey());
                            return;
                        }
                    }
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    java.awt.Point logicalPoint = toLogicalPoint(event.getPoint());
                    int diameter = nodeDiameter();
                    for (Map.Entry<Node, java.awt.Point> entry : positions.entrySet()) {
                        if (entry.getValue().distance(logicalPoint) <= diameter / 2.0 + 5) {
                            Node node = entry.getKey();
                            setToolTipText("Value " + node.value + (node == highlighted
                                    ? " · selected"
                                    : completedNodes.contains(node) ? " · completed" : " · not visited"));
                            return;
                        }
                    }
                    setToolTipText(nodeCount > 35
                            ? "Dense tree: zoom in to read values. Scroll or use the zoom controls; drag to pan."
                            : "Tree diagram. Green single-ring nodes are not visited, teal double-ring nodes are completed, and the gold diamond node is selected.");
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (dragStart == null || zoom <= 1.0) {
                        return;
                    }
                    int deltaX = event.getX() - dragStart.x;
                    int deltaY = event.getY() - dragStart.y;
                    if (Math.abs(deltaX) + Math.abs(deltaY) > 4) {
                        dragged = true;
                    }
                    panX = dragPanX + deltaX;
                    panY = dragPanY + deltaY;
                    repaint();
                }

                @Override
                public void mouseWheelMoved(java.awt.event.MouseWheelEvent event) {
                    double factor = event.getWheelRotation() < 0 ? 1.12 : 1.0 / 1.12;
                    setZoom(zoom * factor, event.getX(), event.getY());
                    event.consume();
                }
            };
            addMouseListener(interaction);
            addMouseMotionListener(interaction);
            addMouseWheelListener(interaction);
        }

        void setTree(Node root, Node highlighted) {
            boolean changedRoot = this.root != root;
            Set<Node> previousNodes = new HashSet<>(knownNodes);
            this.root = root;
            this.highlighted = highlighted;
            this.completedNodes = Collections.emptySet();
            this.tracePath = Collections.emptyList();
            celebrationNode = null;
            pathAnimationTimer.stop();
            rememberNodes(root);
            clearFlash();
            if (changedRoot) {
                resetView();
            } else if (!reducedMotion) {
                for (Node node : knownNodes) {
                    if (!previousNodes.contains(node)) {
                        animateInsertion(node);
                        break;
                    }
                }
            }
            repaint();
        }

        void setTraversalProgress(Node root, Node highlighted, Set<Node> completedNodes) {
            boolean changedRoot = this.root != root;
            this.root = root;
            this.highlighted = highlighted;
            this.completedNodes = new HashSet<>(completedNodes);
            this.tracePath = Collections.emptyList();
            celebrationNode = null;
            pathAnimationTimer.stop();
            rememberNodes(root);
            clearFlash();
            if (changedRoot) {
                resetView();
            }
            repaint();
        }

        void setTracePath(Node root, List<Node> path, Node highlighted) {
            boolean changedRoot = this.root != root;
            this.root = root;
            this.highlighted = highlighted;
            this.tracePath = new ArrayList<>(path);
            this.completedNodes = new HashSet<>(path);
            celebrationNode = null;
            rememberNodes(root);
            if (this.tracePath.size() > 1 && !reducedMotion) {
                pathAnimationTimer.start();
            } else {
                pathAnimationTimer.stop();
            }
            clearFlash();
            if (changedRoot) {
                resetView();
            }
            repaint();
        }

        void celebratePath(List<Node> path) {
            setTracePath(root, path, path.isEmpty() ? null : path.get(path.size() - 1));
            if (!path.isEmpty()) {
                celebrationNode = path.get(path.size() - 1);
                flashNode(celebrationNode, new Color(244, 190, 65));
            }
        }

        void refreshAppearance() {
            if (reducedMotion) {
                insertionTimer.stop();
                animatingNode = null;
                insertionScale = 1f;
                pathAnimationTimer.stop();
                clearFlash();
            } else {
                if (!tracePath.isEmpty() && tracePath.size() > 1) {
                    pathAnimationTimer.start();
                }
            }
            repaint();
        }

        private void animateInsertion(Node node) {
            insertionTimer.stop();
            animatingNode = node;
            insertionScale = 0.12f;
            insertionTimer.start();
        }

        private void rememberNodes(Node node) {
            knownNodes.clear();
            collectNodes(node, knownNodes);
        }

        private void collectNodes(Node node, Set<Node> nodes) {
            if (node == null) {
                return;
            }
            nodes.add(node);
            collectNodes(node.left, nodes);
            collectNodes(node.right, nodes);
        }

        private void resetView() {
            zoom = 1.0;
            panX = 0;
            panY = 0;
            notifyZoomChanged();
        }

        void zoomBy(double factor) {
            setZoom(zoom * factor, getWidth() / 2.0, getHeight() / 2.0);
        }

        void resetZoom() {
            resetView();
            repaint();
        }

        void setZoomChangedListener(java.util.function.IntConsumer listener) {
            zoomChangedListener = listener;
            notifyZoomChanged();
        }

        private void setZoom(double requestedZoom, double anchorX, double anchorY) {
            double oldZoom = zoom;
            zoom = Math.max(1.0, Math.min(4.0, requestedZoom));
            if (zoom == oldZoom) {
                return;
            }
            double centerX = getWidth() / 2.0;
            double centerY = getHeight() / 2.0;
            double logicalX = (anchorX - centerX - panX) / oldZoom + centerX;
            double logicalY = (anchorY - centerY - panY) / oldZoom + centerY;
            panX = anchorX - centerX - zoom * (logicalX - centerX);
            panY = anchorY - centerY - zoom * (logicalY - centerY);
            if (zoom == 1.0) {
                panX = 0;
                panY = 0;
            }
            notifyZoomChanged();
            repaint();
        }

        private void notifyZoomChanged() {
            zoomChangedListener.accept((int) Math.round(zoom * 100));
        }

        private java.awt.Point toLogicalPoint(java.awt.Point point) {
            double centerX = getWidth() / 2.0;
            double centerY = getHeight() / 2.0;
            return new java.awt.Point(
                    (int) ((point.x - centerX - panX) / zoom + centerX),
                    (int) ((point.y - centerY - panY) / zoom + centerY));
        }

        void flashNode(Node node, Color color) {
            if (node == null) {
                return;
            }
            if (reducedMotion) {
                clearFlash();
                return;
            }
            flashingNode = node;
            flashColor = color;
            flashStrength = 1f;
            flashTimer.restart();
            repaint();
        }

        private void clearFlash() {
            flashingNode = null;
            flashStrength = 0f;
            flashTimer.stop();
        }

        void setNodeClickListener(java.util.function.Consumer<Node> listener) {
            this.nodeClickListener = listener;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            positions.clear();
            inorderPositions.clear();
            nodeCount = countNodes(root);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(250, 249, 239),
                    0, getHeight(), new Color(233, 237, 219)));
            g.fillRect(0, 0, getWidth(), getHeight());
            if (root == null || nodeCount == 0) {
                g.setColor(MUTED);
                g.setFont(BODY_FONT.deriveFont(Font.ITALIC, 13f));
                String emptyMessage = "Your tree will appear here";
                int textWidth = g.getFontMetrics().stringWidth(emptyMessage);
                g.drawString(emptyMessage, (getWidth() - textWidth) / 2, getHeight() / 2);
            } else {
                assignInorderPositions(root, new int[] {0});
                assignCoordinates(root, 0, Math.max(1, treeDepth(root)));
                g.translate(getWidth() / 2.0 + panX, getHeight() / 2.0 + panY);
                g.scale(zoom, zoom);
                g.translate(-getWidth() / 2.0, -getHeight() / 2.0);
                g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawEdges(g, root);
                drawTracePath(g);
                drawNodes(g, root);
                if (celebrationNode != null) {
                    drawTreasureMarker(g, positions.get(celebrationNode));
                }
                if (flashingNode != null && flashStrength > 0f) {
                    drawFeedbackEffect(g);
                }
                g.setTransform(((Graphics2D) graphics).getTransform());
                if (nodeCount > 20) {
                    drawOverview(g);
                }
                g.setColor(MUTED);
                g.setFont(BODY_FONT.deriveFont(Font.PLAIN, 11f));
                if (nodeCount > 35 && nodeDiameter() * zoom < 18) {
                    g.setColor(TEXT);
                    g.setFont(BODY_FONT.deriveFont(Font.BOLD, 11f));
                    g.drawString("Dense tree · zoom in to read values", 12, 18);
                }
            }
            drawLegend(g);
            g.dispose();
        }

        private void drawFeedbackEffect(Graphics2D g) {
            java.awt.Point point = positions.get(flashingNode);
            if (point == null) {
                return;
            }
            int diameter = nodeDiameter();
            int expansion = Math.round((1f - flashStrength) * 22f);
            int ringSize = diameter + 8 + expansion;
            Color effect = flashColor == null ? SUCCESS : flashColor;
            g.setColor(new Color(effect.getRed(), effect.getGreen(), effect.getBlue(),
                    Math.max(0, Math.min(190, Math.round(190 * flashStrength)))));
            g.setStroke(new BasicStroke(Math.max(1f, 3f * flashStrength)));
            g.drawOval(point.x - ringSize / 2, point.y - ringSize / 2, ringSize, ringSize);
            if (effect.equals(SUCCESS)) {
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 2 + Math.PI / 4;
                    int inner = diameter / 2 + 6;
                    int outer = inner + 5 + expansion / 2;
                    g.drawLine(point.x + (int) (Math.cos(angle) * inner),
                            point.y + (int) (Math.sin(angle) * inner),
                            point.x + (int) (Math.cos(angle) * outer),
                            point.y + (int) (Math.sin(angle) * outer));
                }
            }
        }

        private void drawTreasureMarker(Graphics2D g, java.awt.Point point) {
            if (point == null) {
                return;
            }
            int x = point.x + nodeDiameter() / 2 + 8;
            int y = Math.max(14, point.y - nodeDiameter() / 2 - 15);
            g.setColor(new Color(255, 235, 154, 145));
            g.fillOval(x - 12, y - 12, 24, 24);
            g.setColor(new Color(139, 88, 37));
            g.fillRoundRect(x - 8, y - 2, 16, 10, 3, 3);
            g.setColor(new Color(218, 159, 55));
            g.fillRoundRect(x - 9, y - 6, 18, 6, 4, 4);
            g.setColor(new Color(112, 74, 37));
            g.drawRoundRect(x - 8, y - 2, 16, 10, 3, 3);
            g.setColor(new Color(255, 224, 122));
            g.fillRect(x - 1, y - 2, 2, 5);
        }

        private int nodeDiameter() {
            int targetSize = nodeCount > 35 ? 22 : nodeCount > 20 ? 28 : 38;
            if (largeTreeVisuals) {
                targetSize += 8;
            }
            int horizontalSpacing = getWidth() / Math.max(1, nodeCount);
            return Math.max(6, Math.min(targetSize, (int) (horizontalSpacing * 0.78)));
        }

        private void drawOverview(Graphics2D g) {
            int overviewWidth = 112;
            int overviewHeight = 78;
            int x = getWidth() - overviewWidth - 12;
            int y = 24;
            g.setColor(new Color(255, 252, 241, 225));
            g.fillRoundRect(x, y, overviewWidth, overviewHeight, 12, 12);
            g.setColor(new Color(166, 171, 145));
            g.drawRoundRect(x, y, overviewWidth, overviewHeight, 12, 12);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 9f));
            g.setColor(MUTED);
            g.drawString("TREE OVERVIEW", x + 7, y + 12);

            int contentX = x + 7;
            int contentY = y + 18;
            int contentWidth = overviewWidth - 14;
            int contentHeight = overviewHeight - 24;
            g.setStroke(new BasicStroke(0.8f));
            g.setColor(new Color(134, 148, 113, 150));
            for (Node node : positions.keySet()) {
                java.awt.Point from = positions.get(node);
                for (Node child : new Node[] {node.left, node.right}) {
                    if (child == null) {
                        continue;
                    }
                    java.awt.Point to = positions.get(child);
                    g.drawLine(contentX + from.x * contentWidth / Math.max(1, getWidth()),
                            contentY + from.y * contentHeight / Math.max(1, getHeight()),
                            contentX + to.x * contentWidth / Math.max(1, getWidth()),
                            contentY + to.y * contentHeight / Math.max(1, getHeight()));
                }
            }
            for (Map.Entry<Node, java.awt.Point> entry : positions.entrySet()) {
                java.awt.Point point = entry.getValue();
                int dotX = contentX + point.x * contentWidth / Math.max(1, getWidth());
                int dotY = contentY + point.y * contentHeight / Math.max(1, getHeight());
                Node node = entry.getKey();
                g.setColor(node == highlighted ? WARNING
                        : completedNodes.contains(node) ? COMPLETED_NODE : UNVISITED_NODE);
                g.fillOval(dotX - 2, dotY - 2, 4, 4);
            }

            double centerX = getWidth() / 2.0;
            double centerY = getHeight() / 2.0;
            double left = (0 - centerX - panX) / zoom + centerX;
            double top = (0 - centerY - panY) / zoom + centerY;
            double right = (getWidth() - centerX - panX) / zoom + centerX;
            double bottom = (getHeight() - centerY - panY) / zoom + centerY;
            int viewLeft = contentX + (int) (left * contentWidth / Math.max(1, getWidth()));
            int viewTop = contentY + (int) (top * contentHeight / Math.max(1, getHeight()));
            int viewRight = contentX + (int) (right * contentWidth / Math.max(1, getWidth()));
            int viewBottom = contentY + (int) (bottom * contentHeight / Math.max(1, getHeight()));
            viewLeft = Math.max(contentX, Math.min(contentX + contentWidth - 3, viewLeft));
            viewTop = Math.max(contentY, Math.min(contentY + contentHeight - 3, viewTop));
            viewRight = Math.max(viewLeft + 3, Math.min(contentX + contentWidth, viewRight));
            viewBottom = Math.max(viewTop + 3, Math.min(contentY + contentHeight, viewBottom));
            g.setColor(new Color(177, 121, 37, 145));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRect(viewLeft, viewTop, viewRight - viewLeft, viewBottom - viewTop);
        }

        private void drawLegend(Graphics2D g) {
            String[] labels = {"Not visited", "Completed", "Selected"};
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 11f));
            int totalWidth = 0;
            for (String label : labels) {
                totalWidth += 22 + g.getFontMetrics().stringWidth(label) + 16;
            }
            int x = Math.max(12, (getWidth() - totalWidth) / 2);
            int y = getHeight() - 15;
            for (int i = 0; i < labels.length; i++) {
                drawStateMarker(g, x + 5, y - 5, i, 10);
                g.setColor(TEXT);
                g.drawString(labels[i], x + 15, y);
                x += 22 + g.getFontMetrics().stringWidth(labels[i]) + 16;
            }
        }

        private void drawStateMarker(Graphics2D g, int centerX, int centerY, int state, int size) {
            int radius = size / 2;
            if (state == 2) {
                g.setColor(new Color(242, 193, 78, 100));
                g.fillOval(centerX - radius - 3, centerY - radius - 3, size + 6, size + 6);
                g.setColor(new Color(147, 100, 29));
                g.setStroke(new BasicStroke(1.5f));
                g.drawPolygon(new int[] {centerX, centerX + radius + 2, centerX, centerX - radius - 2},
                        new int[] {centerY - radius - 2, centerY, centerY + radius + 2, centerY}, 4);
            }
            g.setColor(state == 0 ? UNVISITED_NODE : state == 1 ? COMPLETED_NODE : ACTIVE_NODE);
            g.fillOval(centerX - radius, centerY - radius, size, size);
            g.setColor(state == 0 ? new Color(228, 241, 225)
                    : state == 1 ? new Color(218, 245, 235) : TEXT);
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval(centerX - radius, centerY - radius, size, size);
            if (state == 1) {
                g.drawOval(centerX - radius + 2, centerY - radius + 2, size - 4, size - 4);
            } else if (state == 2) {
                g.setColor(new Color(147, 100, 29));
                g.setStroke(new BasicStroke(1.5f));
                g.drawPolygon(new int[] {centerX, centerX + radius + 2, centerX, centerX - radius - 2},
                        new int[] {centerY - radius - 2, centerY, centerY + radius + 2, centerY}, 4);
            }
        }

        private void drawTracePath(Graphics2D g) {
            if (tracePath.size() < 2) {
                return;
            }
            float alpha = reducedMotion ? 1f : 0.72f + 0.28f
                    * (float) Math.sin(pathAnimationPhase * Math.PI / 6.0);
            g.setColor(new Color(222, 136, 45, Math.max(0, Math.min(255, Math.round(255 * alpha)))));
            g.setStroke(reducedMotion
                    ? new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                    : new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                            10f, new float[] {8f, 5f}, pathAnimationPhase));
            for (int i = 1; i < tracePath.size(); i++) {
                java.awt.Point from = positions.get(tracePath.get(i - 1));
                java.awt.Point to = positions.get(tracePath.get(i));
                g.drawLine(from.x, from.y, to.x, to.y);
            }
        }

        private int countNodes(Node node) {
            if (node == null) {
                return 0;
            }
            return 1 + countNodes(node.left) + countNodes(node.right);
        }

        private void assignInorderPositions(Node node, int[] position) {
            if (node == null) {
                return;
            }
            assignInorderPositions(node.left, position);
            inorderPositions.put(node.value, position[0]++);
            assignInorderPositions(node.right, position);
        }

        private void assignCoordinates(Node node, int depth, int maxDepth) {
            if (node == null) {
                return;
            }
            int index = inorderPositions.get(node.value);
            int horizontalInset = Math.min(30, Math.max(12, getWidth() / 20));
            int usableWidth = Math.max(1, getWidth() - horizontalInset * 2);
            int x = horizontalInset + (int) ((index + 0.5) * usableWidth / nodeCount);
            int topInset = Math.min(34, Math.max(24, getHeight() / 8));
            int bottomInset = 38 + nodeDiameter() / 2;
            int usableHeight = Math.max(1, getHeight() - topInset - bottomInset);
            int y = topInset + depth * usableHeight / maxDepth;
            positions.put(node, new java.awt.Point(x, y));
            assignCoordinates(node.left, depth + 1, maxDepth);
            assignCoordinates(node.right, depth + 1, maxDepth);
        }

        private int treeDepth(Node node) {
            if (node == null) {
                return 0;
            }
            return 1 + Math.max(treeDepth(node.left), treeDepth(node.right));
        }

        private void drawEdges(Graphics2D g, Node node) {
            if (node == null) {
                return;
            }
            java.awt.Point from = positions.get(node);
            g.setColor(new Color(134, 148, 113));
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (node.left != null) {
                java.awt.Point to = positions.get(node.left);
                g.setColor(new Color(156, 126, 99));
                g.drawLine(from.x, from.y, to.x, to.y);
                drawBranchLabel(g, from, to, "L", new Color(133, 99, 74));
                drawEdges(g, node.left);
            }
            if (node.right != null) {
                java.awt.Point to = positions.get(node.right);
                g.setColor(new Color(102, 143, 117));
                g.drawLine(from.x, from.y, to.x, to.y);
                drawBranchLabel(g, from, to, "R", new Color(67, 113, 83));
                drawEdges(g, node.right);
            }
        }

        private void drawBranchLabel(Graphics2D g, java.awt.Point from, java.awt.Point to,
                String label, Color color) {
            if (from.distance(to) * zoom < 48) {
                return;
            }
            int x = (from.x * 2 + to.x) / 3;
            int y = (from.y * 2 + to.y) / 3;
            int width = 17;
            int height = 15;
            g.setColor(new Color(255, 252, 241, 235));
            g.fillRoundRect(x - width / 2, y - height / 2, width, height, 8, 8);
            g.setColor(color);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x - width / 2, y - height / 2, width, height, 8, 8);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, 9f));
            int textWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, x - textWidth / 2, y + g.getFontMetrics().getAscent() / 3);
        }

        private void drawNodes(Graphics2D g, Node node) {
            if (node == null) {
                return;
            }
            java.awt.Point point = positions.get(node);
            int diameter = nodeDiameter();
            Graphics2D parentGraphics = g;
            Graphics2D nodeGraphics = (Graphics2D) g.create();
            if (node == animatingNode && insertionScale < 1f) {
                nodeGraphics.translate(point.x, point.y);
                nodeGraphics.scale(insertionScale, insertionScale);
                nodeGraphics.translate(-point.x, -point.y);
            }
            g = nodeGraphics;
            boolean active = node == highlighted;
            boolean completed = completedNodes.contains(node);
            boolean flashing = node == flashingNode && flashStrength > 0f;
            if (active) {
                g.setColor(new Color(242, 193, 78, 65));
                g.fillOval(point.x - diameter / 2 - 10, point.y - diameter / 2 - 10,
                        diameter + 20, diameter + 20);
                g.setColor(new Color(147, 100, 29));
                g.setStroke(new BasicStroke(1.7f));
                g.drawPolygon(new int[] {point.x, point.x + diameter / 2 + 6,
                        point.x, point.x - diameter / 2 - 6},
                        new int[] {point.y - diameter / 2 - 6, point.y,
                                point.y + diameter / 2 + 6, point.y}, 4);
            }
            g.setColor(new Color(30, 48, 36, 45));
            g.fillOval(point.x - diameter / 2 + 2, point.y - diameter / 2 + 3,
                    diameter, diameter);
            Color nodeColor = active ? ACTIVE_NODE : completed ? COMPLETED_NODE : UNVISITED_NODE;
            if (flashing) {
                nodeColor = blend(nodeColor, flashColor, flashStrength * 0.82f);
            }
            g.setColor(nodeColor);
            g.fillOval(point.x - diameter / 2, point.y - diameter / 2, diameter, diameter);
            g.setColor(active ? new Color(147, 100, 29)
                    : completed ? new Color(218, 245, 235) : new Color(228, 241, 225));
            g.setStroke(new BasicStroke(active ? 2.5f : 1.5f));
            g.drawOval(point.x - diameter / 2, point.y - diameter / 2, diameter, diameter);
            if (completed && diameter * zoom >= 18 && diameter >= 4) {
                g.drawOval(point.x - diameter / 2 + 1, point.y - diameter / 2 + 1,
                        diameter - 2, diameter - 2);
            }
            g.setColor(active ? TEXT : Color.WHITE);
            float labelSize = Math.max(4f, Math.min(largeTreeVisuals ? 15f : 12f,
                    diameter * 0.56f + (largeTreeVisuals ? 1.5f : 0f)));
            String value = Integer.toString(node.value);
            g.setFont(BODY_FONT.deriveFont(Font.BOLD, labelSize));
            int width = g.getFontMetrics().stringWidth(value);
            int minimumLabelSize = largeTreeVisuals ? 16 : 18;
            if (diameter * zoom >= minimumLabelSize
                    && width <= (getWidth() / Math.max(1, nodeCount) - 1) * zoom) {
                g.drawString(value, point.x - width / 2, point.y + g.getFontMetrics().getAscent() / 3);
            }
            if (active) {
                g.setColor(new Color(147, 100, 29));
                g.setStroke(new BasicStroke(2.5f));
                g.drawOval(point.x - diameter / 2 - 2, point.y - diameter / 2 - 2,
                        diameter + 4, diameter + 4);
            }
            drawNodes(parentGraphics, node.left);
            drawNodes(parentGraphics, node.right);
            nodeGraphics.dispose();
        }

        private Color blend(Color base, Color overlay, float amount) {
            float inverse = 1f - amount;
            return new Color(
                    Math.round(base.getRed() * inverse + overlay.getRed() * amount),
                    Math.round(base.getGreen() * inverse + overlay.getGreen() * amount),
                    Math.round(base.getBlue() * inverse + overlay.getBlue() * amount));
        }
    }

    public static void main(String[] args) {
        if (!configureRandom(args)) {
            System.exit(2);
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("BST Adventure requires a graphical desktop environment. "
                    + "Run it on a machine with a display attached.");
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException exception) {
                System.err.println("Could not load the game look and feel: " + exception.getMessage());
            }
            new GameWindow().setVisible(true);
        });
    }

    private static boolean configureRandom(String[] args) {
        if (args.length == 0) {
            return true;
        }
        if (args.length == 2 && args[0].equals("--seed")) {
            try {
                random = new Random(Long.parseLong(args[1]));
                return true;
            } catch (NumberFormatException e) {
                System.err.println("Seed must be a whole number.");
            }
        }
        System.err.println("Usage: java Main [--seed <number>]");
        return false;
    }

    static Node createRandomTree(int count, Random random) {
        if (count < 0 || count > 90) {
            throw new IllegalArgumentException("Tree size must be between 0 and 90.");
        }
        Node root = null;
        Set<Integer> used = new HashSet<>();
        while (used.size() < count) {
            used.add(random.nextInt(90) + 10);
        }
        List<Integer> insertionOrder = new ArrayList<>(used);
        Collections.shuffle(insertionOrder, random);
        for (int value : insertionOrder) {
            root = insert(root, value);
        }
        return root;
    }

    static Node insert(Node root, int value) {
        if (root == null) {
            return new Node(value);
        }

        Node current = root;
        while (true) {
            if (value < current.value) {
                if (current.left == null) {
                    current.left = new Node(value);
                    break;
                }
                current = current.left;
            } else if (value > current.value) {
                if (current.right == null) {
                    current.right = new Node(value);
                    break;
                }
                current = current.right;
            } else {
                break;
            }
        }
        return root;
    }

    static boolean search(Node root, int value) {
        Node current = root;
        while (current != null) {
            if (current.value == value) {
                return true;
            }
            current = value < current.value ? current.left : current.right;
        }
        return false;
    }

    static List<Integer> inorderValues(Node root) {
        List<Integer> values = new ArrayList<>();
        NodeStack nodes = new NodeStack();
        Node current = root;
        while (current != null || !nodes.isEmpty()) {
            while (current != null) {
                nodes.push(current);
                current = current.left;
            }
            current = nodes.pop();
            values.add(current.value);
            current = current.right;
        }
        return values;
    }

    static List<Node> traversalNodes(Node root, TraversalOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Traversal order must not be null.");
        }
        List<Node> nodes = new ArrayList<>();
        if (root == null) {
            return nodes;
        }

        NodeStack stack = new NodeStack();
        switch (order) {
            case PREORDER -> {
                stack.push(root);
                while (!stack.isEmpty()) {
                    Node node = stack.pop();
                    nodes.add(node);
                    if (node.right != null) {
                        stack.push(node.right);
                    }
                    if (node.left != null) {
                        stack.push(node.left);
                    }
                }
            }
            case INORDER -> {
                Node current = root;
                while (current != null || !stack.isEmpty()) {
                    while (current != null) {
                        stack.push(current);
                        current = current.left;
                    }
                    current = stack.pop();
                    nodes.add(current);
                    current = current.right;
                }
            }
            case POSTORDER -> {
                NodeStack reverse = new NodeStack();
                stack.push(root);
                while (!stack.isEmpty()) {
                    Node node = stack.pop();
                    reverse.push(node);
                    if (node.left != null) {
                        stack.push(node.left);
                    }
                    if (node.right != null) {
                        stack.push(node.right);
                    }
                }
                while (!reverse.isEmpty()) {
                    nodes.add(reverse.pop());
                }
            }
            default -> throw new IllegalStateException("Unsupported traversal order: " + order);
        }
        return nodes;
    }

    static List<Integer> traversalValues(Node root, TraversalOrder order) {
        List<Integer> values = new ArrayList<>();
        for (Node node : traversalNodes(root, order)) {
            values.add(node.value);
        }
        return values;
    }

    static class PracticeSearchRewards {
        private final Set<Integer> rewardedValues = new HashSet<>();

        boolean recordSuccessfulSearch(int value) {
            return rewardedValues.add(value);
        }
    }

}
