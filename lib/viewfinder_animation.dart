import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';

/// Shows an animated viewfinder frame.
class ViewfinderAnimation extends StatefulWidget {
  final Size size;
  final Duration duration;
  final Color color;

  const ViewfinderAnimation({
    Key key,
    this.size,
    this.duration = const Duration(milliseconds: 1200),
    this.color = Colors.white,
  }) : super(key: key);

  @override
  _ViewfinderAnimationState createState() => _ViewfinderAnimationState();
}

class _ViewfinderAnimationState extends State<ViewfinderAnimation>
    with SingleTickerProviderStateMixin {
  AnimationController _controller;
  Animation<double> _animation;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();

    _controller = AnimationController(vsync: this, duration: widget.duration)
      ..addListener(() => setState(() {}))
      ..repeat(reverse: true);

    _animation = Tween(begin: 1.18, end: 1.25)
        .animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
  }

  @override
  Widget build(BuildContext context) {
    return Transform.scale(
      scale: _animation.value,
      child: CustomPaint(
        size: widget.size,
        painter: ViewfinderPainter(color: widget.color),
      ),
    );
  }
}

class ViewfinderPainter extends CustomPainter {
  final Color color;

  ViewfinderPainter({this.color});

  @override
  void paint(Canvas canvas, Size size) {
    var w = size.width;
    var h = size.height;
    Paint paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;

    Path path_0 = Path()
      ..moveTo(w * 1 / 7, h * 2 / 7)
      ..lineTo(w * 1 / 7, h * 1 / 7)
      ..lineTo(w * 2 / 7, h * 1 / 7);

    Path path_1 = Path()
      ..moveTo(w * 5 / 7, h * 1 / 7)
      ..lineTo(w * 6 / 7, h * 1 / 7)
      ..lineTo(w * 6 / 7, h * 2 / 7);

    Path path_2 = Path()
      ..moveTo(w * 6 / 7, h * 5 / 7)
      ..lineTo(w * 6 / 7, h * 6 / 7)
      ..lineTo(w * 5 / 7, h * 6 / 7);

    Path path_3 = Path()
      ..moveTo(w * 2 / 7, h * 6 / 7)
      ..lineTo(w * 1 / 7, h * 6 / 7)
      ..lineTo(w * 1 / 7, h * 5 / 7);

    [path_0, path_1, path_2, path_3]
        .forEach((path) => canvas.drawPath(path, paint));
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => false;
}
