extern crate piston_window;
extern crate gfx_device_gl;
extern crate find_folder;
extern crate gfx_graphics;
extern crate gfx;
extern crate nalgebra;
extern crate ncollide;

use piston_window::*;
use gfx_device_gl::{Resources};

use nalgebra::Vec2 as Vector2;
type Vec2 = Vector2<f64>;

mod object;
use object::Object;
use object::Tank;
use object::Bullet;

struct Game {
    player1: Tank,
    player2: Tank,
    bullets: Vec<Bullet>,
    hull_destroyed: Option<Texture<Resources>>,
    turret_destroyed: Option<Texture<Resources>>,
    bullet: Option<Texture<Resources>>,
    up_d: bool, down_d: bool, left_d: bool, right_d: bool,
    scx: f64, scy: f64
}

impl Game {
    fn new() -> Game {
        Game { player1 : Tank::new(), player2: Tank::new(), bullets: Vec::<Bullet>::new(), up_d: false, down_d: false, left_d: false, right_d: false, scx: 300.0, scy: 300.0,
            hull_destroyed: None, turret_destroyed: None, bullet: None }
    }
    fn on_load(&mut self, w: &PistonWindow) {
        let assets = find_folder::Search::ParentsThenKids(3, 3).for_folder("assets").unwrap();
        let tank_sprite = assets.join("E-100_Base.png");
        let tank_sprite = Texture::from_path(
                &mut *w.factory.borrow_mut(),
                &tank_sprite,
                Flip::None,
                &TextureSettings::new())
                .unwrap();
        let tank_turret = assets.join("E-100_Turret.png");
        let tank_turret = Texture::from_path(
                &mut *w.factory.borrow_mut(),
                &tank_turret,
                Flip::None,
                &TextureSettings::new())
                .unwrap();
        let tank_dest_sprite = assets.join("E-100_Base_destroyed.png");
        let tank_dest_sprite = Texture::from_path(
                &mut *w.factory.borrow_mut(),
                &tank_dest_sprite,
                Flip::None,
                &TextureSettings::new())
                .unwrap();
        let tank_dest_turret = assets.join("E-100_Turret_destroyed.png");
        let tank_dest_turret = Texture::from_path(
                &mut *w.factory.borrow_mut(),
                &tank_dest_turret,
                Flip::None,
                &TextureSettings::new())
                .unwrap();
        let bulsprite = assets.join("Bullet.png");
        let bulsprite = Texture::from_path(
                &mut *w.factory.borrow_mut(),
                &bulsprite,
                Flip::None,
                &TextureSettings::new())
                .unwrap();
        self.hull_destroyed = Some(tank_dest_sprite);
        self.turret_destroyed = Some(tank_dest_turret);
        self.bullet = Some(bulsprite);

        self.player1.hull.set_sprite(tank_sprite.clone());
        self.player1.turret.set_sprite(tank_turret.clone());
        self.player2.hull.set_sprite(tank_sprite.clone());
        self.player2.turret.set_sprite(tank_turret.clone());
        self.player2.mov_to(Vec2::new(300.0, 0.0));
    }
    fn on_update(&mut self, upd: UpdateArgs) {
        for bul in &mut self.bullets {
            if self.player1.collides(&bul) {
                self.player1.is_destroyed = true;
                self.player1.hull.set_sprite(self.hull_destroyed.clone().unwrap());
                self.player1.turret.set_sprite(self.turret_destroyed.clone().unwrap());
                bul.to_be_removed = true;
            }
            if self.player2.collides(&bul) {
                self.player2.is_destroyed = true;
                self.player2.hull.set_sprite(self.hull_destroyed.clone().unwrap());
                self.player2.turret.set_sprite(self.turret_destroyed.clone().unwrap());
                bul.to_be_removed = true;
            }
            bul.update(upd.dt);
        }
        self.bullets.retain(|ref bul| bul.to_be_removed == false);
        if self.up_d && !self.player1.is_destroyed {
            self.player1.fwd(150.0 * upd.dt);
        }
        if self.down_d && !self.player1.is_destroyed {
            self.player1.fwd(-150.0 * upd.dt);
        }
        if self.left_d && !self.player1.is_destroyed {
            self.player1.rot(-1.0 * upd.dt);
        }
    }
    fn on_draw(&mut self, ren: RenderArgs, e: PistonWindow) {
        self.scx = (ren.width / 2) as f64;
        self.scy = (ren.height / 2) as f64;
        e.draw_2d(|c, g| {
            clear([0.8, 0.8, 0.8, 1.0], g);
            let center = c.transform.trans(self.scx, self.scy);
            self.player1.render(g, center);
            self.player2.render(g, center);
            for bul in &self.bullets {
                bul.render(g, center);
            }
        });
    }
    fn on_input(&mut self, inp: Input) {
        match inp {
            Input::Press(but) => {
                match but {
                    Button::Keyboard(Key::Up) => {
                        self.up_d = true;
                    }
                    Button::Keyboard(Key::Down) => {
                        self.down_d = true;
                    }
                    Button::Keyboard(Key::Left) => {
                        self.left_d = true;
                    }
                    Button::Keyboard(Key::Right) => {
                        self.right_d = true;
                    }
                    _ => {}
                }
            }
            Input::Release(but) => {
                match but {
                    Button::Keyboard(Key::Up) => {
                        self.up_d = false;
                    }
                    Button::Keyboard(Key::Down) => {
                        self.down_d = false;
                    }
                    Button::Keyboard(Key::Left) => {
                        self.left_d = false;
                    }
                    Button::Keyboard(Key::Right) => {
                        self.right_d = false;
                    }
                    Button::Mouse(MouseButton::Left) => {
                        self.bullets.push(self.player1.fire(self.bullet.clone().unwrap()));
                    }
                    _ => {}
                }
            }
            Input::Move(mot) => {
                match mot {
                    Motion::MouseCursor(x, y) => {
                        self.player1.point_tur_to(x - self.scx, y - self.scy);
                    }
                    _ => {}
                }
            }
            _ => {}
        }
    }
}

fn main() {
    let window: PistonWindow = WindowSettings::new(
        "piston-tutorial",
        [600, 600]
    )
    .exit_on_esc(true)
    .build()
    .unwrap();
    let mut game = Game::new();
    game.on_load(&window);
    for e in window {
        match e.event {
            Some(Event::Update(upd)) => {
                game.on_update(upd);
            }
            Some(Event::Render(ren)) => {
                game.on_draw(ren, e);
            }
            Some(Event::Input(inp)) => {
                game.on_input(inp);
            }
            _ => {

            }
        }
    }
}
