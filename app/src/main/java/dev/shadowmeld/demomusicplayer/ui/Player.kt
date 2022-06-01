package dev.shadowmeld.demomusicplayer.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.logger
import com.google.accompanist.glide.rememberGlidePainter
import dev.shadowmeld.viewdaydream.ui.now_player.SliderWithLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@ExperimentalMaterialApi
@Composable
fun ParentPlayer(viewModel: MainViewModel? = null) {

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "now_player_screen") {
        composable("now_player_screen") { NowPlayerScreen(navController, viewModel) }
    }
}


@ExperimentalMaterialApi
@Composable
private fun NowPlayerScreen(
    navController: NavController? = null,
    viewModel: MainViewModel? = null
) {
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden
    )
    val scope = rememberCoroutineScope()
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            BottomSheetContent(viewModel, scope, sheetState)
        },
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = colorResource(id = R.color.color_primary),
    ) {
        MainScreen(scope = scope, state = sheetState, viewModel = viewModel)
    }
}

@ExperimentalMaterialApi
@Preview
@Composable
private fun PreviewGreeting() {
    NowPlayerScreen()
}

@ExperimentalMaterialApi
@Composable
private fun MainScreen(
    scope: CoroutineScope,
    state: ModalBottomSheetState,
    navController: NavController? = null,
    viewModel: MainViewModel? = null
) {
    val musicData = remember {
        mutableStateOf<LocalMusicInfo?>(null)
    }

    var sliderPosition by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription =
                        "Back"
                        )
                    }
                },
                actions = {
                    /* RowScope 会将这些 Icon 水平放置 */
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "菜单")
                    }
                },
                backgroundColor = Color.Transparent,
                elevation = 0.dp
            )
        },
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Color.White
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp, 8.dp, 36.dp, 8.dp)
                    .padding(0.dp, 36.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = 16.dp
            ) {
                Image(
                    painter = rememberGlidePainter(R.mipmap.ic_launcher),
                    contentDescription = "Contact profile picture",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            } // 封面

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(36.dp, 8.dp, 36.dp, 8.dp)
            ) {

                val (favorite, musicInfo, libraryAdd) = createRefs()

                Column(
                    modifier = Modifier
                        .constrainAs(musicInfo) {
                            start.linkTo(parent.start)
                            width = Dimension.fillToConstraints
                        }
                ) {
                    Text(
                        text = musicData.value?.musicName ?: "Music Name",
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = musicData.value?.musicArtist ?: "Music Artist",
                        style = MaterialTheme.typography.body2
                    )
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .constrainAs(favorite) {
                            end.linkTo(parent.end)
                        }) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = "喜欢")
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .constrainAs(libraryAdd) {
                            end.linkTo(favorite.start)
                        }) {
                    Icon(painterResource(id = R.drawable.ic_outline_library_add), contentDescription = "喜欢")
                }

            } // 音乐信息

            SliderWithLabel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp, 8.dp, 36.dp, 8.dp),
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {

                },
                steps = 200,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.secondary,
                    activeTrackColor = MaterialTheme.colors.secondary,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                finiteEnd = true,
                valueRange = 0f..100f
            ) // 进度条

            ConstraintLayout(
                modifier = Modifier
                    .height(46.dp)
                    .fillMaxWidth()
                    .padding(36.dp, 8.dp, 36.dp, 8.dp)
            ) {

                val (play, next, previous, shuffle, repeat) = createRefs()

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .constrainAs(repeat) {
                            start.linkTo(parent.start)
                            end.linkTo(previous.start)
                        }) {
                    Icon(painterResource(R.drawable.ic_round_repeat), contentDescription = "喜欢")
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .constrainAs(previous) {
                            start.linkTo(repeat.end)
                            end.linkTo(play.start)
                        }) {
                    Icon(painterResource(R.drawable.ic_outline_skip_previous), contentDescription = "喜欢")
                }

                IconButton(
                    onClick = {
                        logger("viewModel == ${viewModel == null} | playButton == ${viewModel?.playButton == null}")

                        viewModel?.playButton?.invoke()
                    },
                    modifier = Modifier
                        .constrainAs(play) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }) {
                    Icon(painterResource(R.drawable.ic_round_play_arrow), contentDescription = "喜欢")
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .constrainAs(next) {
                            start.linkTo(play.end)
                            end.linkTo(shuffle.start)
                        }) {
                    Icon(painterResource(R.drawable.ic_outline_skip_next), contentDescription = "喜欢")
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .constrainAs(shuffle) {
                            start.linkTo(next.end)
                            end.linkTo(parent.end)
                        }) {
                    Icon(painterResource(R.drawable.ic_round_shuffle), contentDescription = "喜欢")
                }

                createHorizontalChain(repeat, previous, play, next, shuffle, chainStyle = ChainStyle.SpreadInside)
            } // 播放控制

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, false),
            ) {

                Divider(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    //颜色
                    color = colorResource(id = R.color.strokeColor),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp, 8.dp, 36.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Row(
                        modifier = Modifier
                            .width(0.dp)
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier
                                .background(
                                    colorResource(id = R.color.scrim),
                                    RoundedCornerShape(40.dp)
                                )
                                .padding(2.dp),
                            painter = painterResource(id = R.drawable.ic_outline_volume_up_24),
                            contentDescription = "",
                        )

                        Text(
                            text = "正在播放的设备",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp, end = 16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                state.show()
                            }
                        }){
                        Icon(painterResource(R.drawable.ic_round_playlist_play), contentDescription = "喜欢")
                    }

                }
            } // 播放设备及列表
        }
    }
}


@ExperimentalMaterialApi
@Composable
fun BottomSheetContent(
    viewModel: MainViewModel? = null,
    scope: CoroutineScope,
    state: ModalBottomSheetState
) {
    val context = LocalContext.current

    viewModel?.setMusicInfoRepository(context)
    val list = viewModel?.getMusicInfo()

    list?.let {
        LazyColumn {
            // Add a single item
            items(it) { musicInfo ->
                BottomSheetListItem(
                    position = "0",
                    icon = R.drawable.ic_round_shuffle,
                    title = musicInfo.title,
                    onItemClick = { title ->

                        scope.launch {
                            state.hide()
                        }
                        Toast.makeText(
                            context,
                            title,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
            }
        }
    }

}

@Composable
fun BottomSheetListItem(position: String, icon: Int, title: String, onItemClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                onItemClick(title)
            })
            .height(55.dp)
            .background(color = colorResource(id = R.color.white))
            .padding(start = 15.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = position)
        Image(painter = painterResource(id = icon), contentDescription = "Music")
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title)
    }
}

@Preview(showBackground = true)
@Composable
fun BottomSheetListItemPreview() {
    BottomSheetListItem(position = "0",icon = R.drawable.ic_outline_playlist_play_24, title = "Share", onItemClick = {})
}

