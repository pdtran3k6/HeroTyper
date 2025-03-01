package com.phatphoophoo.pdtran.herotyper.presenters

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.phatphoophoo.pdtran.herotyper.R
import com.phatphoophoo.pdtran.herotyper.activities.GameActivity
import com.phatphoophoo.pdtran.herotyper.models.GAME_DIFFICULTY
import com.phatphoophoo.pdtran.herotyper.models.GameScreenModel
import com.phatphoophoo.pdtran.herotyper.objects.GameObject
import com.phatphoophoo.pdtran.herotyper.objects.PlayerObject
import com.phatphoophoo.pdtran.herotyper.services.BulletService
import com.phatphoophoo.pdtran.herotyper.services.EnemyService
import com.phatphoophoo.pdtran.herotyper.services.HealthGainService
import com.phatphoophoo.pdtran.herotyper.services.PowerupService
import com.phatphoophoo.pdtran.herotyper.services.StatsService
import com.phatphoophoo.pdtran.herotyper.views.GameScreenView
import com.phatphoophoo.pdtran.herotyper.views.ScrollingBGView

class GameScreenPresenter(
    private val gameActivity: GameActivity,
    private val gameScreenView: GameScreenView,
    private val keyboardGamePresenter: KeyboardGamePresenter,
    private val windowSize: Pair<Float, Float>,
    difficulty: GAME_DIFFICULTY
) {
    companion object {
        // Const values
        const val REFRESH_RATE: Long = 50 // In MS
    }

    private var lastXPos: Float = windowSize.first / 2
    private var gameModel: GameScreenModel = GameScreenModel()
    private val enemyService: EnemyService = EnemyService(difficulty, windowSize)
    private val bulletService: BulletService = BulletService()
    private val healthGainService: HealthGainService = HealthGainService(windowSize)
    private val powerupService: PowerupService = PowerupService()

    private var missile: Button
    private var missile_txt: TextView
    private var words: Int = 0
    private var gameTimer: GameTimer?

    var gamePaused: Boolean = false
        set(newVal) {
            if (newVal) {
                scrollingBg.animator.pause()
                gameActivity.soundService.pauseSound()
            } else {
                scrollingBg.animator.resume()
                gameActivity.soundService.resumeSound()
            }
            field = newVal
        }

    private val keyboardTxtView: TextView
    private val keyboardView: LinearLayout
    private val gameHandler: Handler = Handler(Looper.getMainLooper())
    private val gameLooper: Runnable = Runnable { gameLoop() }
    private val scrollingBg: ScrollingBGView = gameActivity.findViewById(R.id.scrolling_content)

    init {
        scrollingBg.animator.start()
        gameModel.playerObject = PlayerObject(Pair(lastXPos, windowSize.second - PlayerObject.size))
        keyboardTxtView = gameActivity.findViewById(R.id.curWordTextView) as TextView
        keyboardView = gameActivity.findViewById(R.id.custom_keyboard_keys_large) as LinearLayout
        //Initialize game
        gameHandler.post(gameLooper)

        //Start game timer
        gameTimer = GameTimer()

        gameScreenView.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            // Update the position within screen constraints
            lastXPos = Math.max(Math.min(motionEvent.x, windowSize.first - 250), 50f)
            true
        }

        // pause button event listener
        val btn = gameActivity.findViewById(R.id.pause) as Button
        btn.setOnClickListener {
            gamePaused = !gamePaused
            if (gamePaused) {
                gameTimer!!.pauseTimer()
                gameHandler.removeCallbacks(gameLooper)
                gameActivity.showPauseFragment()
            } else {
                gameActivity.hidePauseFragment()
                resumeGame()
            }
            keyboardGamePresenter.toggleInput(gamePaused)
        }

        // set up missiles count display + event listener
        missile = gameActivity.findViewById(R.id.missile)
        missile_txt = gameActivity.findViewById(R.id.num_missiles)
        missile_txt.text = "x ${gameModel.numMissiles}"
        missile.setOnClickListener {
            if (gameModel.numMissiles > 0) {
                // Clear screen
                for (enemy in gameModel.enemies) {
                    gameModel.score += enemy.scoreValue
                    enemy.isDestroyed = true
                    gameActivity.soundService.playSound(R.raw.blast)
                }
                gameModel.numMissiles -= 1

            }
            missile_txt.text = "x ${gameModel.numMissiles}"
            if (gameModel.numMissiles == 0) {
                missile.alpha = 0.3f
            }
        }

        // set the start for measuring wpm.
        words = 0

        gameActivity.soundService.stopSound(R.raw.battle_loop) // Stop potential layering of bgm
        gameActivity.soundService.playBackgroundMusic(R.raw.battle_loop)
    }

    // Where the Game tells various helper classes to update the state of the game,
    // while the Game itself handles logic not able to be handled solely by the object
    // type in question
    private fun gameLoop() {
        // Update the state of the game objects
        gameModel.playerObject.position = Pair(lastXPos, windowSize.second - PlayerObject.size)

        gameModel.enemies = enemyService.updateEnemies(gameModel.enemies)

        gameModel.healthGainObjects = healthGainService.updateHealthGainObjects(gameModel.healthGainObjects)
        gameModel.powerups = powerupService.updatePowerup(gameModel.powerups, null)

        // Check for completed words to fire bullets
        if (keyboardGamePresenter.hasWordCompleted()) {
            val bulletPos = Pair(
                gameModel.playerObject.position.first,
                gameModel.playerObject.position.second
            )

            gameModel.bullets = bulletService.updateBullets(gameModel.bullets, bulletPos)

            // add to the number of words typed.
            words += 1

            gameActivity.soundService.playSound(R.raw.shot_fired)
        } else {
            gameModel.bullets = bulletService.updateBullets(gameModel.bullets)
        }

        // Check for barrier collisions
        val livesLost = enemyService.popHitStack()
        val livesGained = handleHealthGainObjectHit(gameModel)
        val livesLeft = gameModel.lives - livesLost + livesGained

        // play sound for the number of enemies hit the base.
        if (livesLost > 0) {
            gameActivity.soundService.playSound(R.raw.base_explosion)
        }

        gameModel.lives = livesLeft

        // Check for bullet-enemy collisions
        handleEnemyHit(gameModel)

        // Check for powerup-user collisions
        handlePowerupHit(gameModel)

        // Update the view
        gameScreenView.setModel(gameModel)

        if (livesLeft <= 0) {
            endGame()
        } else {
            gameHandler.postDelayed(gameLooper, REFRESH_RATE)
        }
    }

    // Marks all objects as destroyed; they can-self handle this
    // in the next game loop
    private fun handleEnemyHit(gameModel: GameScreenModel) {
        val newEnemyList = gameModel.enemies.filter { !it.isDestroyed }

        gameModel.bullets.forEach { bullet ->
            var collided: Boolean

            for(enemy in newEnemyList) {
                collided = checkCollision(bullet, enemy)

                if (collided) {
                    gameModel.score += enemy.scoreValue
                    enemy.isDestroyed = true
                    bullet.isDestroyed = true
                    gameActivity.soundService.playSound(R.raw.asteroid_explosion)
                    // Generate a random power-up
                    // see PowerupService for probability of each power-up
                    val powerupPos = Pair(enemy.position.first, enemy.position.second)
                    gameModel.powerups = powerupService.updatePowerup(gameModel.powerups, powerupPos)
                    break
                }
            }
        }
    }

    //Returns lives gained
    private fun handleHealthGainObjectHit(gameModel: GameScreenModel): Int {
        val liveHealthGainObjects = gameModel.healthGainObjects.filter { healthGainObject -> !healthGainObject.isDestroyed && !healthGainObject.isRewarded }
        val playerObject = gameModel.playerObject

        var rewardCount = 0
        liveHealthGainObjects.forEach{ healthGainObject ->
            healthGainObject.isRewarded = checkCollision(playerObject, healthGainObject)

            if(healthGainObject.isRewarded){
                rewardCount += 1
                gameActivity.soundService.playSound(R.raw.plasma_explode)
            }
        }

        return rewardCount
    }

    private fun checkCollision(obj1: GameObject, obj2: GameObject): Boolean {
        return (obj1.position.first <= obj2.position.first + obj2.width &&
            obj1.position.first + obj1.width >= obj2.position.first) &&
        // Y collision
        (obj1.position.second <= obj2.position.second + obj2.height &&
            obj1.position.second >= obj2.position.second)
    }

    private fun handlePowerupHit(gameModel: GameScreenModel) {
        val playerObj = gameModel.playerObject
        gameModel.powerups.forEach { pup ->
            val collided = checkCollision(pup, playerObj)

            if (collided) {
                pup.isDestroyed = true
                gameModel.numMissiles += 1
                missile.alpha = 1f
                missile_txt.text = "x ${gameModel.numMissiles}"
                gameActivity.soundService.playSound(R.raw.plasma_explode)
            }
        }
    }

    fun resumeGame(){
        gameTimer!!.resumeTimer()
        gameHandler.postDelayed(gameLooper, REFRESH_RATE)
        gamePaused = false
    }

    fun endGame() {
        val totalTime = gameTimer!!.endTimer()
        this.gameTimer = null
        gamePaused = true

        //Get Hit/Miss data
        val keysHitMissMap = keyboardGamePresenter.getKeysHitMissMap()

        //Save stats info
        StatsService.setKeysMap(keysHitMissMap)
        StatsService.setWpm(((words * 60 * 1000) / totalTime).toInt())
        StatsService.write()

        // Stop the game loop from posting so we pause
        gameHandler.removeCallbacks(gameLooper)
        gameActivity.showGameOverFragment()
    }

    // GameTimer is meant for single game use
    class GameTimer {
        private var currFrameStartTime: Long? = 0
        private var currTimeSoFar: Long = 0
        private var timerCompleted: Boolean = false
        private val invalidUseError = Error("Invalid use of timer")

        init {
            currFrameStartTime = System.currentTimeMillis()
        }

        fun pauseTimer() {
            if (timerCompleted) {
                throw invalidUseError
            }
            currTimeSoFar += System.currentTimeMillis() - currFrameStartTime!!
            currFrameStartTime = null
        }

        fun resumeTimer() {
            if (timerCompleted) {
                throw invalidUseError
            }
            currFrameStartTime = System.currentTimeMillis()
        }

        //Only call this when game ends
        fun endTimer(): Long {
            if (timerCompleted) {
                throw invalidUseError
            }
            if (currFrameStartTime != null) {
                currTimeSoFar += System.currentTimeMillis() - currFrameStartTime!!
            }

            return currTimeSoFar
        }
    }
}