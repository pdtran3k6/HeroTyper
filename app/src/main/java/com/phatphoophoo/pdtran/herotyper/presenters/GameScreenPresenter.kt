package com.phatphoophoo.pdtran.herotyper.presenters

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import com.phatphoophoo.pdtran.herotyper.R
import com.phatphoophoo.pdtran.herotyper.activities.GameActivity
import com.phatphoophoo.pdtran.herotyper.services.EnemyService
import com.phatphoophoo.pdtran.herotyper.models.GAME_DIFFICULTY
import com.phatphoophoo.pdtran.herotyper.models.GameScreenModel
import com.phatphoophoo.pdtran.herotyper.objects.PlayerObject
import com.phatphoophoo.pdtran.herotyper.services.BulletService
import com.phatphoophoo.pdtran.herotyper.services.StatsService
import com.phatphoophoo.pdtran.herotyper.views.GameScreenView
import com.phatphoophoo.pdtran.herotyper.views.ScrollingBGView


class GameScreenPresenter(
    val gameActivity: GameActivity,
    val gameScreenView: GameScreenView,
    val customKeyboardPresenter: CustomKeyboardPresenter,
    val windowSize: Pair<Float,Float>,
    val difficulty: GAME_DIFFICULTY
) {
    companion object {
        // Const values
        const val REFRESH_RATE : Long = 50 // In MS
    }

    var lastXPos: Float = windowSize.first/2
    var gameModel : GameScreenModel = GameScreenModel()
    val enemyService: EnemyService = EnemyService(difficulty, windowSize)
    val bulletService: BulletService = BulletService()
    var words : Int = 0
    var start : Long = 0
    // the total time playing the game (not counting paused)
    var totalTime : Long = 0

    private val gameHandler : Handler = Handler(Looper.getMainLooper())
    private val gameLooper : Runnable = Runnable { gameLoop() }
    private val scrollingBg : ScrollingBGView = gameActivity.findViewById(R.id.scrolling_content)

    init {
        scrollingBg.animator.start()
        gameModel.playerObject = PlayerObject(Pair(lastXPos, windowSize.second - 200))
        gameHandler.post(gameLooper)

        gameScreenView.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            // Update the position within screen constraints
            lastXPos = Math.max(Math.min(motionEvent.x, windowSize.first - 250), 50f)
            true
        }

        // pause button event listener
        val id = gameActivity.resources.getIdentifier("pause", "id", PACKAGE_NAME)
        val btn = gameActivity.findViewById(id) as Button
        btn.setOnClickListener{
            gameHandler.removeCallbacks(gameLooper)
            gameActivity.showPauseFragment()
            scrollingBg.animator.pause()
        }

//        set the start for measuring wpm.
        start = System.currentTimeMillis()
        words = 0
        totalTime = 0

    }

    // Where the Game tells various helper classes to update the state of the game,
    // while the Game itself handles logic not able to be handled solely by the object
    // type in question
    fun gameLoop() {
        // Update the state of the game objects
        gameModel.playerObject!!.position = Pair(lastXPos, gameModel.playerObject!!.position.second)

        gameModel.enemies = enemyService.updateEnemies(gameModel.enemies)

        // Check for completed words to fire bullets
        if(customKeyboardPresenter.hasWordCompleted()) {
            var bulletPos = Pair(gameModel.playerObject!!.position.first + 50,
                gameModel.playerObject!!.position.second)

            gameModel.bullets = bulletService.updateBullets(gameModel.bullets, bulletPos)

            // add to the number of words typed.
            words += 1

        } else {
            gameModel.bullets = bulletService.updateBullets(gameModel.bullets)
        }

        // Check for barrier collisions
        val livesLost = enemyService.popHitStack()
        val livesLeft = gameModel.lives - livesLost

        gameModel.lives = livesLeft

        // Check for bullet-enemy collisions
        handleEnemyHit(gameModel)

        // Update the view
        gameScreenView.setModel(gameModel)

        if (livesLeft <= 0){
            endGame()
        }
        else {
            gameHandler.postDelayed(gameLooper, REFRESH_RATE)
        }
    }

    // Marks all objects as destroyed; they can-self handle this
    // in the next game loop
    fun handleEnemyHit(gameModel: GameScreenModel) {
        val newEnemyList = gameModel.enemies.filter{ !it.isDestroyed }

        val newBulletList = gameModel.bullets.forEach { bullet ->
            var collided = false
            var curIndex = 0

            for(enemy in newEnemyList) {
                collided =
                    // X collision
                    (bullet.position.first <= enemy.position.first + enemy.width &&
                            bullet.position.first + bullet.width >= enemy.position.first) &&
                    // Y collision
                    (bullet.position.second <= enemy.position.second + enemy.height &&
                            bullet.position.second >= enemy.position.second)

                if (collided) {
                    gameModel.score += enemy.scoreValue
                    enemy.isDestroyed = true
                    bullet.isDestroyed = true
                    break
                }
                curIndex++
            }
        }
    }

    fun resumeGame(){
        // update the start time
        start = System.currentTimeMillis()

        gameHandler.postDelayed(gameLooper, REFRESH_RATE)
        scrollingBg.animator.resume()
    }

    fun endGame() {
        // Stop the game loop from posting so we pause
        gameHandler.removeCallbacks(gameLooper)
        gameActivity.showGameOverFragment()
    }

    // Call when the game ends
    fun dispose() {

    }
}