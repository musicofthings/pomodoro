package com.example.cozyfocus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cozyfocus.model.CompanionAnimal
import com.example.cozyfocus.model.Cosmetic

val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun AnimalFace(
    companion: CompanionAnimal,
    cosmetic: Cosmetic?,
    modifier: Modifier = Modifier
) {
    val fur = when (companion) {
        CompanionAnimal.RED_PANDA -> Color(0xFFFF9233)
        CompanionAnimal.CAPYBARA -> Color(0xFF8D5B34)
        CompanionAnimal.RABBIT -> Color(0xFFD4C2B3)
        CompanionAnimal.PUPPY -> Color(0xFFC2874D)
        CompanionAnimal.CAT -> Color(0xFF94949E)
        CompanionAnimal.HORSE -> Color(0xFF734024)
    }

    val innerEar = if (companion == CompanionAnimal.RABBIT) {
        Color(0xFFFFB6C1).copy(alpha = 0.55f)
    } else {
        fur.copy(alpha = 0.65f)
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val side = maxWidth

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Ears
            EarsLayer(companion = companion, fur = fur, innerEar = innerEar, side = side)

            // Face Body
            FaceLayer(companion = companion, fur = fur, side = side)

            // Face Markings
            FaceMarkingsLayer(companion = companion, side = side)

            // Eyes
            EyesLayer(companion = companion, side = side)

            // Muzzle & Nose
            MuzzleLayer(companion = companion, fur = fur, side = side)

            // Cosmetic Mark on Forehead
            if (cosmetic != null) {
                Text(
                    text = cosmetic.mark,
                    fontSize = (side.value * 0.28f).sp,
                    modifier = Modifier.offset(y = (-side.value * 0.47f).dp)
                )
            }
        }
    }
}

@Composable
private fun EarsLayer(
    companion: CompanionAnimal,
    fur: Color,
    innerEar: Color,
    side: Dp
) {
    val s = side.value
    when (companion) {
        CompanionAnimal.RABBIT -> {
            Row(
                modifier = Modifier.offset(y = (-s * 0.23f).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((s * 0.26f).dp, (s * 0.72f).dp)
                        .rotate(-8f)
                        .background(fur, CircleShape)
                        .padding((s * 0.07f).dp)
                        .background(innerEar, CircleShape)
                )
                Spacer(modifier = Modifier.size((s * 0.14f).dp))
                Box(
                    modifier = Modifier
                        .size((s * 0.26f).dp, (s * 0.72f).dp)
                        .rotate(8f)
                        .background(fur, CircleShape)
                        .padding((s * 0.07f).dp)
                        .background(innerEar, CircleShape)
                )
            }
        }
        CompanionAnimal.PUPPY -> {
            Row(
                modifier = Modifier.offset(y = (-s * 0.14f).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((s * 0.25f).dp, (s * 0.56f).dp)
                        .rotate(-25f)
                        .background(fur.copy(alpha = 0.88f), CircleShape)
                )
                Spacer(modifier = Modifier.size((s * 0.50f).dp))
                Box(
                    modifier = Modifier
                        .size((s * 0.25f).dp, (s * 0.56f).dp)
                        .rotate(25f)
                        .background(fur.copy(alpha = 0.88f), CircleShape)
                )
            }
        }
        CompanionAnimal.CAPYBARA -> {
            Row(
                modifier = Modifier.offset(y = (-s * 0.28f).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((s * 0.24f).dp)
                        .background(fur, CircleShape)
                )
                Spacer(modifier = Modifier.size((s * 0.52f).dp))
                Box(
                    modifier = Modifier
                        .size((s * 0.24f).dp)
                        .background(fur, CircleShape)
                )
            }
        }
        else -> {
            Row(
                modifier = Modifier.offset(y = (-s * 0.29f).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((s * 0.52f).dp, (s * 0.55f).dp)
                        .rotate(-8f)
                        .background(fur, TriangleShape)
                )
                Spacer(modifier = Modifier.size((s * 0.35f).dp - (s * 0.17f).dp))
                Box(
                    modifier = Modifier
                        .size((s * 0.52f).dp, (s * 0.55f).dp)
                        .rotate(8f)
                        .background(fur, TriangleShape)
                )
            }
        }
    }
}

@Composable
private fun FaceLayer(
    companion: CompanionAnimal,
    fur: Color,
    side: Dp
) {
    val s = side.value
    when (companion) {
        CompanionAnimal.HORSE -> {
            Box(
                modifier = Modifier
                    .offset(y = (s * 0.05f).dp)
                    .size((s * 0.68f).dp, (s * 1.02f).dp)
                    .background(fur, RoundedCornerShape((s * 0.40f).dp))
            )
        }
        CompanionAnimal.CAPYBARA -> {
            Box(
                modifier = Modifier
                    .offset(y = (s * 0.07f).dp)
                    .size((s * 0.90f).dp, (s * 0.75f).dp)
                    .background(fur, RoundedCornerShape((s * 0.34f).dp))
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .offset(y = (s * 0.05f).dp)
                    .size((s * 0.94f).dp, (s * 0.90f).dp)
                    .background(fur, CircleShape)
            )
        }
    }
}

@Composable
private fun FaceMarkingsLayer(
    companion: CompanionAnimal,
    side: Dp
) {
    val s = side.value
    when (companion) {
        CompanionAnimal.RED_PANDA -> {
            Row(
                modifier = Modifier.offset(y = (s * 0.08f).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((s * 0.26f).dp, (s * 0.38f).dp)
                        .rotate(27f)
                        .background(Color.White.copy(alpha = 0.86f), CircleShape)
                )
                Spacer(modifier = Modifier.size((s * 0.14f).dp))
                Box(
                    modifier = Modifier
                        .size((s * 0.26f).dp, (s * 0.38f).dp)
                        .rotate(-27f)
                        .background(Color.White.copy(alpha = 0.86f), CircleShape)
                )
            }
        }
        CompanionAnimal.CAT -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-s * 0.15f).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((s * 0.045f).dp, (s * 0.22f).dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                )
                Spacer(modifier = Modifier.size((s * 0.05f).dp))
                Box(
                    modifier = Modifier
                        .size((s * 0.035f).dp, (s * 0.13f).dp)
                        .background(Color.Black.copy(alpha = 0.28f), CircleShape)
                )
            }
        }
        CompanionAnimal.HORSE -> {
            Box(
                modifier = Modifier
                    .offset(y = (-s * 0.12f).dp)
                    .size((s * 0.18f).dp, (s * 0.48f).dp)
                    .background(Color.White.copy(alpha = 0.70f), CircleShape)
            )
        }
        else -> {}
    }
}

@Composable
private fun EyesLayer(
    companion: CompanionAnimal,
    side: Dp
) {
    val s = side.value
    val spacing = if (companion == CompanionAnimal.HORSE) (s * 0.22f).dp else (s * 0.27f).dp
    val offsetY = if (companion == CompanionAnimal.HORSE) (-s * 0.03f).dp else (-s * 0.02f).dp

    Row(
        modifier = Modifier.offset(y = offsetY)
    ) {
        SingleEye(side = side)
        Spacer(modifier = Modifier.size(spacing))
        SingleEye(side = side)
    }
}

@Composable
private fun SingleEye(side: Dp) {
    val s = side.value
    Box(
        modifier = Modifier
            .size((s * 0.15f).dp, (s * 0.18f).dp)
            .background(Color.Black, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .offset(x = (s * 0.035f).dp, y = (s * 0.035f).dp)
                .size((s * 0.04f).dp)
                .background(Color.White.copy(alpha = 0.92f), CircleShape)
        )
    }
}

@Composable
private fun MuzzleLayer(
    companion: CompanionAnimal,
    fur: Color,
    side: Dp
) {
    val s = side.value
    val offsetY = if (companion == CompanionAnimal.HORSE) (s * 0.24f).dp else (s * 0.25f).dp
    val muzzleWidth = if (companion == CompanionAnimal.HORSE) (s * 0.42f).dp else (s * 0.52f).dp
    val muzzleColor = if (companion == CompanionAnimal.CAPYBARA) fur.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.78f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = offsetY)
    ) {
        Box(
            modifier = Modifier
                .size(muzzleWidth, (s * 0.28f).dp)
                .background(muzzleColor, RoundedCornerShape((s * 0.20f).dp))
        )
        Box(
            modifier = Modifier
                .offset(y = (-s * 0.03f).dp)
                .size((s * 0.12f).dp, (s * 0.08f).dp)
                .background(Color.Black.copy(alpha = 0.83f), CircleShape)
        )
    }
}
